package com.pet.vetalya.medshop.auth.service;

import com.pet.vetalya.medshop.auth.dto.AuthDtos.TokenResponse;
import com.pet.vetalya.medshop.auth.entity.StoreAuthProvider;
import com.pet.vetalya.medshop.auth.repository.StoreAuthProviderRepository;
import com.pet.vetalya.medshop.common.MedShopException;
import com.pet.vetalya.medshop.tenant.entity.Tenant;
import com.pet.vetalya.medshop.tenant.repository.TenantRepository;
import com.pet.vetalya.medshop.user.entity.StoreUser;
import com.pet.vetalya.medshop.user.repository.StoreUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Service
public class StoreSocialLoginService {

    private static final Logger log = LoggerFactory.getLogger(StoreSocialLoginService.class);

    private final StoreAuthProviderRepository authProviderRepo;
    private final StoreUserRepository userRepo;
    private final TenantRepository tenantRepo;
    private final StoreTokenService tokenService;

    public StoreSocialLoginService(
            StoreAuthProviderRepository authProviderRepo,
            StoreUserRepository userRepo,
            TenantRepository tenantRepo,
            StoreTokenService tokenService) {
        this.authProviderRepo = authProviderRepo;
        this.userRepo = userRepo;
        this.tenantRepo = tenantRepo;
        this.tokenService = tokenService;
    }

    @Transactional
    public TokenResponse processSocialLogin(String provider, String sub, String email,
                                           boolean emailVerified, String name,
                                           String clientType, String deviceId, String ipAddress) {
        // 1. Find existing linked provider
        Optional<StoreAuthProvider> existingProvider = authProviderRepo.findByProviderAndProviderUserId(provider, sub);
        if (existingProvider.isPresent()) {
            StoreAuthProvider authProv = existingProvider.get();
            StoreUser user = userRepo.findByTenantIdAndId(authProv.getTenantId(), authProv.getUserId())
                    .orElseThrow(() -> new MedShopException.UnauthorizedException("User account not found"));

            if (!user.isActive()) {
                throw new MedShopException.UnauthorizedException("User account is deactivated");
            }
            Tenant tenant = tenantRepo.findById(user.getTenantId())
                    .orElseThrow(() -> new MedShopException.UnauthorizedException("Tenant store not found"));
            if (!tenant.isActive()) {
                throw new MedShopException.UnauthorizedException("Store is deactivated");
            }

            user.setLastLogin(Instant.now());
            userRepo.save(user);

            String accessToken = tokenService.createAccessToken(user);
            String refreshToken = tokenService.createRefreshToken(user, null, clientType, deviceId, ipAddress);
            return TokenResponse.of(accessToken, refreshToken, 900);
        }

        // 2. If email_verified is true and a medshop.users row exists with the same email, LINK the provider
        if (emailVerified && email != null && !email.isBlank()) {
            Optional<StoreUser> userByEmail = userRepo.findByEmail(email.trim().toLowerCase());
            if (userByEmail.isPresent()) {
                StoreUser user = userByEmail.get();
                if (!user.isActive()) {
                    throw new MedShopException.UnauthorizedException("User account is deactivated");
                }
                Tenant tenant = tenantRepo.findById(user.getTenantId())
                        .orElseThrow(() -> new MedShopException.UnauthorizedException("Tenant store not found"));
                if (!tenant.isActive()) {
                    throw new MedShopException.UnauthorizedException("Store is deactivated");
                }

                // Link provider to existing staff account
                StoreAuthProvider newLink = new StoreAuthProvider(user.getTenantId(), user.getId(), provider, sub, email);
                authProviderRepo.save(newLink);

                user.setLastLogin(Instant.now());
                userRepo.save(user);

                log.info("Linked social identity {} ({}) to existing user {}", provider, sub, user.getId());
                String accessToken = tokenService.createAccessToken(user);
                String refreshToken = tokenService.createRefreshToken(user, null, clientType, deviceId, ipAddress);
                return TokenResponse.of(accessToken, refreshToken, 900);
            }
        }

        // 3. New identity: Never auto-create inside an existing shop. Return ONBOARDING_REQUIRED
        String onboardingToken = tokenService.createOnboardingToken(provider, sub, email, emailVerified, name);
        return TokenResponse.onboarding(onboardingToken);
    }
}

