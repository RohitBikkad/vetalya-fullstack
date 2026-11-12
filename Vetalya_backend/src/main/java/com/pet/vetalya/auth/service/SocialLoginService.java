package com.pet.vetalya.auth.service;

import com.github.f4b6a3.ulid.UlidCreator;
import com.pet.vetalya.dto.ProviderUserInfo;
import com.pet.vetalya.entity.*;
import com.pet.vetalya.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Set;

@Service
public class SocialLoginService {

    private static final Logger log = LoggerFactory.getLogger(SocialLoginService.class);
    private static final Set<String> ALLOWED_ROLES = Set.of("ROLE_VET", "ROLE_PET_OWNER", "ROLE_PLATFORM_ADMIN", "ROLE_CLINIC_ADMIN");

    private final UserAuthRepository userAuthRepo;
    private final AuthProviderRepository authProviderRepo;
    private final VetRepository vetRepo;
    private final PetOwnerRepository petOwnerRepo;
    private final TokenService tokenService;

    public SocialLoginService(UserAuthRepository userAuthRepo,
                              AuthProviderRepository authProviderRepo,
                              VetRepository vetRepo,
                              PetOwnerRepository petOwnerRepo,
                              TokenService tokenService) {
        this.userAuthRepo = userAuthRepo;
        this.authProviderRepo = authProviderRepo;
        this.vetRepo = vetRepo;
        this.petOwnerRepo = petOwnerRepo;
        this.tokenService = tokenService;
    }

    /**
     * Find-or-create user from provider info, issue tokens.
     * Called by both the OAuth2 redirect success handler and the native login endpoints.
     */
    @Transactional
    public LoginResult processLogin(ProviderUserInfo info, String role, String clientType,
                                    String deviceId, String ipAddress) {
        validateRole(role);

        var existing = authProviderRepo.findByProviderAndProviderUserId(info.provider(), info.sub());

        UserAuth user;
        boolean newUser;

        if (existing.isPresent()) {
            // ── Returning user ──
            user = userAuthRepo.findById(existing.get().getUserId())
                    .orElseThrow(() -> new IllegalStateException("Orphaned auth_provider row"));

            if ("BLOCKED".equals(user.getStatus()) || "DELETED".equals(user.getStatus())) {
                throw new AccountBlockedException("Account is " + user.getStatus().toLowerCase());
            }
            newUser = false;
        } else if (info.email() != null && info.emailVerified()) {
            // ── Email-link: same verified email exists → link this provider ──
            var byEmail = userAuthRepo.findByEmailAndEmailVerified(info.email(), true);
            if (byEmail.isPresent()) {
                user = byEmail.get();
                if ("BLOCKED".equals(user.getStatus()) || "DELETED".equals(user.getStatus())) {
                    throw new AccountBlockedException("Account is " + user.getStatus().toLowerCase());
                }
                linkProvider(user, info);
                newUser = false;
            } else {
                // ── New user ──
                user = createUser(info, role);
                newUser = true;
            }
        } else {
            // ── New user (email not verified or absent) ──
            user = createUser(info, role);
            newUser = true;
        }

        // Update last login
        user.setLastLoginAt(Instant.now());
        userAuthRepo.save(user);

        // Issue tokens
        String accessToken = tokenService.createAccessToken(user);
        String refreshToken = tokenService.createRefreshToken(user, clientType, deviceId, ipAddress);
        boolean profileComplete = isProfileComplete(user);

        return new LoginResult(user, accessToken, refreshToken, newUser, profileComplete);
    }

    private UserAuth createUser(ProviderUserInfo info, String role) {
        String userId = UlidCreator.getMonotonicUlid().toString();

        UserAuth user = new UserAuth();
        user.setUserId(userId);
        user.setRole(role);
        user.setEmail(info.email());
        user.setEmailVerified(info.emailVerified());
        user.setStatus("ACTIVE");
        user.setTokenVersion(0);
        userAuthRepo.save(user);

        linkProvider(user, info);
        createProfileRow(user, info);

        log.info("Created new user {} with role {} via {}", userId, role, info.provider());
        return user;
    }

    private void linkProvider(UserAuth user, ProviderUserInfo info) {
        AuthProvider ap = new AuthProvider();
        ap.setUserId(user.getUserId());
        ap.setProvider(info.provider());
        ap.setProviderUserId(info.sub());
        ap.setProviderEmail(info.email());
        authProviderRepo.save(ap);
    }

    private void createProfileRow(UserAuth user, ProviderUserInfo info) {
        if ("ROLE_VET".equals(user.getRole())) {
            Vet vet = new Vet();
            vet.setUserId(user.getUserId());
            vet.setFirstName(info.firstName());
            vet.setLastName(info.lastName());
            vet.setPhotoUrl(info.pictureUrl());
            vetRepo.save(vet);
        } else {
            PetOwner po = new PetOwner();
            po.setUserId(user.getUserId());
            po.setFirstName(info.firstName());
            po.setLastName(info.lastName());
            petOwnerRepo.save(po);
        }
    }

    private boolean isProfileComplete(UserAuth user) {
        if ("ROLE_VET".equals(user.getRole())) {
            return vetRepo.findById(user.getUserId()).map(v -> v.isProfileComplete()).orElse(false);
        }
        return petOwnerRepo.findById(user.getUserId()).map(o -> o.isProfileComplete()).orElse(false);
    }

    private void validateRole(String role) {
        if (role == null || !ALLOWED_ROLES.contains(role)) {
            throw new IllegalArgumentException("Invalid role: " + role + ". Must be one of: " + ALLOWED_ROLES);
        }
    }

    // ── Result types ──

    public record LoginResult(
            UserAuth user,
            String accessToken,
            String refreshToken,
            boolean newUser,
            boolean profileComplete
    ) {}

    public static class AccountBlockedException extends RuntimeException {
        public AccountBlockedException(String message) { super(message); }
    }
}
