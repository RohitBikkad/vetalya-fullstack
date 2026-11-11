package com.pet.vetalya.medshop.auth.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pet.vetalya.medshop.auth.dto.AuthDtos.*;
import com.pet.vetalya.medshop.auth.entity.AuthExchangeCode;
import com.pet.vetalya.medshop.auth.entity.StoreAuthProvider;
import com.pet.vetalya.medshop.auth.repository.AuthExchangeCodeRepository;
import com.pet.vetalya.medshop.auth.repository.StoreAuthProviderRepository;
import com.pet.vetalya.medshop.common.MedShopException;
import com.pet.vetalya.medshop.common.ValidationPatterns;
import com.pet.vetalya.medshop.security.StoreUserStatusCache;
import com.pet.vetalya.medshop.tenant.entity.Tenant;
import com.pet.vetalya.medshop.tenant.repository.TenantRepository;
import com.pet.vetalya.medshop.user.entity.StoreUser;
import com.pet.vetalya.medshop.user.repository.StoreUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class StoreAuthService {

    private static final Logger log = LoggerFactory.getLogger(StoreAuthService.class);

    private final StoreUserRepository userRepo;
    private final TenantRepository tenantRepo;
    private final StoreAuthProviderRepository authProviderRepo;
    private final AuthExchangeCodeRepository exchangeCodeRepo;
    private final StoreTokenService tokenService;
    private final PasswordEncoder passwordEncoder;
    private final StoreUserStatusCache userStatusCache;
    private final ObjectMapper objectMapper;

    @Value("${app.store.auth.max-login-attempts:5}")
    private int maxLoginAttempts;

    @Value("${app.store.auth.lockout-duration-minutes:15}")
    private int lockoutDurationMinutes;

    // In-memory sliding failure tracking per identifier and per IP
    private final ConcurrentHashMap<String, FailureRecord> failureTracker = new ConcurrentHashMap<>();

    private record FailureRecord(int attempts, Instant lockedUntil) {}

    public StoreAuthService(
            StoreUserRepository userRepo,
            TenantRepository tenantRepo,
            StoreAuthProviderRepository authProviderRepo,
            AuthExchangeCodeRepository exchangeCodeRepo,
            StoreTokenService tokenService,
            PasswordEncoder passwordEncoder,
            StoreUserStatusCache userStatusCache,
            ObjectMapper objectMapper) {
        this.userRepo = userRepo;
        this.tenantRepo = tenantRepo;
        this.authProviderRepo = authProviderRepo;
        this.exchangeCodeRepo = exchangeCodeRepo;
        this.tokenService = tokenService;
        this.passwordEncoder = passwordEncoder;
        this.userStatusCache = userStatusCache;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public TokenResponse login(LoginRequest req, String clientType, String deviceId, String ipAddress) {
        String identifier = req.identifier().trim();
        checkLockout(identifier, ipAddress);

        Optional<StoreUser> userOpt = userRepo.findByUsername(identifier)
                .or(() -> userRepo.findByEmail(identifier.toLowerCase()))
                .or(() -> userRepo.findByPhone(identifier));

        if (userOpt.isEmpty()) {
            recordFailure(identifier, ipAddress);
            throw new MedShopException.UnauthorizedException("Invalid username, email, phone, or password");
        }

        StoreUser user = userOpt.get();
        if (user.getPasswordHash() == null || !passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            recordFailure(identifier, ipAddress);
            throw new MedShopException.UnauthorizedException("Invalid username, email, phone, or password");
        }

        if (!user.isActive()) {
            throw new MedShopException.UnauthorizedException("User account is deactivated");
        }

        Tenant tenant = tenantRepo.findById(user.getTenantId())
                .orElseThrow(() -> new MedShopException.UnauthorizedException("Tenant store not found"));
        if (!tenant.isActive()) {
            throw new MedShopException.UnauthorizedException("Store is deactivated");
        }

        clearFailure(identifier, ipAddress);
        user.setLastLogin(Instant.now());
        userRepo.save(user);

        String accessToken = tokenService.createAccessToken(user);
        String refreshToken = tokenService.createRefreshToken(user, null, clientType, deviceId, ipAddress);
        return TokenResponse.of(accessToken, refreshToken, 900);
    }

    @Transactional
    public TokenResponse registerShop(RegisterShopRequest req, String clientType, String deviceId, String ipAddress) {
        validateShopTaxRegistration(req.gstin(), req.stateCode());

        String email = req.email().trim().toLowerCase();
        String phone = req.phone().trim();

        if (userRepo.existsByEmail(email)) {
            throw new MedShopException.ConflictException("Email is already registered");
        }
        if (userRepo.existsByPhone(phone)) {
            throw new MedShopException.ConflictException("Phone number is already registered");
        }

        String username = (req.username() != null && !req.username().isBlank())
                ? req.username().trim()
                : generateUniqueUsername(email);

        if (userRepo.existsByUsername(username)) {
            throw new MedShopException.ConflictException("Username is already taken");
        }

        // 1. Create Tenant
        Tenant tenant = new Tenant();
        tenant.setShopName(req.shopName().trim());
        tenant.setGstin(req.gstin() != null && !req.gstin().isBlank() ? req.gstin().trim().toUpperCase() : null);
        tenant.setStateCode(req.stateCode().trim());
        tenant.setAddress(req.address().trim());
        tenant.setPhone(phone);
        tenant.setEmail(email);
        tenant.setCompositionDealer(Boolean.TRUE.equals(req.isCompositionDealer()));
        tenant.setActive(true);
        tenant = tenantRepo.save(tenant);

        // 2. Create OWNER User
        StoreUser owner = new StoreUser();
        owner.setTenantId(tenant.getId());
        owner.setUsername(username);
        owner.setEmail(email);
        owner.setPhone(phone);
        owner.setFirstName(req.ownerFirstName().trim());
        owner.setLastName(req.ownerLastName().trim());
        owner.setRole("OWNER");
        owner.setPasswordHash(passwordEncoder.encode(req.password()));
        owner.setActive(true);
        owner.setTokenVersion(0);
        owner.setLastLogin(Instant.now());
        owner = userRepo.save(owner);

        log.info("Registered new shop {} (tenantId={}) with owner {}", tenant.getShopName(), tenant.getId(), owner.getId());

        String accessToken = tokenService.createAccessToken(owner);
        String refreshToken = tokenService.createRefreshToken(owner, null, clientType, deviceId, ipAddress);
        return TokenResponse.of(accessToken, refreshToken, 900);
    }

    @Transactional
    public TokenResponse registerShopSocial(RegisterShopSocialRequest req, String clientType, String deviceId, String ipAddress) {
        Map<String, Object> onboarding = tokenService.parseOnboardingToken(req.onboardingToken());
        String provider = (String) onboarding.get("provider");
        String sub = (String) onboarding.get("sub");
        String email = (String) onboarding.get("email");
        String name = (String) onboarding.get("name");

        validateShopTaxRegistration(req.gstin(), req.stateCode());

        String phone = req.phone().trim();
        if (userRepo.existsByPhone(phone)) {
            throw new MedShopException.ConflictException("Phone number is already registered");
        }

        if (email != null && userRepo.existsByEmail(email.toLowerCase())) {
            throw new MedShopException.ConflictException("Email is already registered");
        }

        String username = (req.username() != null && !req.username().isBlank())
                ? req.username().trim()
                : generateUniqueUsername(email != null ? email : "user_" + sub.substring(0, Math.min(8, sub.length())));

        if (userRepo.existsByUsername(username)) {
            throw new MedShopException.ConflictException("Username is already taken");
        }

        // 1. Create Tenant
        Tenant tenant = new Tenant();
        tenant.setShopName(req.shopName().trim());
        tenant.setGstin(req.gstin() != null && !req.gstin().isBlank() ? req.gstin().trim().toUpperCase() : null);
        tenant.setStateCode(req.stateCode().trim());
        tenant.setAddress(req.address().trim());
        tenant.setPhone(phone);
        tenant.setEmail(email != null ? email : phone + "@medshop.local");
        tenant.setCompositionDealer(Boolean.TRUE.equals(req.isCompositionDealer()));
        tenant.setActive(true);
        tenant = tenantRepo.save(tenant);

        // 2. Parse First/Last name
        String firstName = "Owner";
        String lastName = "User";
        if (name != null && !name.isBlank()) {
            String[] parts = name.trim().split("\\s+", 2);
            firstName = parts[0];
            if (parts.length > 1) lastName = parts[1];
        }

        // 3. Create OWNER User (passwordHash = null for social-only user)
        StoreUser owner = new StoreUser();
        owner.setTenantId(tenant.getId());
        owner.setUsername(username);
        owner.setEmail(tenant.getEmail());
        owner.setPhone(phone);
        owner.setFirstName(firstName);
        owner.setLastName(lastName);
        owner.setRole("OWNER");
        owner.setPasswordHash(null);
        owner.setActive(true);
        owner.setTokenVersion(0);
        owner.setLastLogin(Instant.now());
        owner = userRepo.save(owner);

        // 4. Save AuthProvider link
        StoreAuthProvider authProvider = new StoreAuthProvider(tenant.getId(), owner.getId(), provider, sub, email);
        authProviderRepo.save(authProvider);

        log.info("Registered shop {} via social identity {} with owner {}", tenant.getShopName(), provider, owner.getId());

        String accessToken = tokenService.createAccessToken(owner);
        String refreshToken = tokenService.createRefreshToken(owner, null, clientType, deviceId, ipAddress);
        return TokenResponse.of(accessToken, refreshToken, 900);
    }

    @Transactional
    public String createExchangeCode(Object payload) {
        try {
            byte[] rawBytes = new byte[32];
            new SecureRandom().nextBytes(rawBytes);
            String code = Base64.getUrlEncoder().withoutPadding().encodeToString(rawBytes);
            String hash = StoreTokenService.sha256(code);

            String json = objectMapper.writeValueAsString(payload);
            AuthExchangeCode exchangeCode = new AuthExchangeCode(hash, json, Instant.now().plus(60, ChronoUnit.SECONDS));
            exchangeCodeRepo.save(exchangeCode);
            return code;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to create exchange code", e);
        }
    }

    @Transactional
    public Object exchangeCode(String code) {
        String hash = StoreTokenService.sha256(code.trim());
        AuthExchangeCode exchangeCode = exchangeCodeRepo.findByCodeHash(hash)
                .orElseThrow(() -> new MedShopException.UnauthorizedException("Invalid or expired exchange code"));

        if (exchangeCode.isUsed() || exchangeCode.isExpired()) {
            throw new MedShopException.UnauthorizedException("Exchange code expired or already used");
        }

        exchangeCode.setUsedAt(Instant.now());
        exchangeCodeRepo.save(exchangeCode);

        try {
            return objectMapper.readValue(exchangeCode.getPayload(), Object.class);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to deserialize exchange code payload", e);
        }
    }

    @Transactional
    public void changePassword(String tenantId, String userId, ChangePasswordRequest req) {
        StoreUser user = userRepo.findByTenantIdAndId(tenantId, userId)
                .orElseThrow(() -> new MedShopException.NotFoundException("User not found"));

        if (user.getPasswordHash() != null) {
            if (req.currentPassword() == null || req.currentPassword().isBlank()) {
                throw new MedShopException.BadRequestException("Current password is required");
            }
            if (!passwordEncoder.matches(req.currentPassword(), user.getPasswordHash())) {
                throw new MedShopException.BadRequestException("Current password does not match");
            }
        }

        user.setPasswordHash(passwordEncoder.encode(req.newPassword()));
        user.setTokenVersion(user.getTokenVersion() + 1);
        userRepo.save(user);

        tokenService.revokeAllForUser(tenantId, userId);
        userStatusCache.evict(userId);
    }

    @Transactional
    public void linkProvider(String tenantId, String userId, String provider, String sub, String email) {
        Optional<StoreAuthProvider> existing = authProviderRepo.findByProviderAndProviderUserId(provider, sub);
        if (existing.isPresent()) {
            throw new MedShopException.ConflictException("This social account is already linked to another user");
        }
        StoreAuthProvider newLink = new StoreAuthProvider(tenantId, userId, provider, sub, email);
        authProviderRepo.save(newLink);
    }

    @Transactional
    public void unlinkProvider(String tenantId, String userId, String provider) {
        StoreUser user = userRepo.findByTenantIdAndId(tenantId, userId)
                .orElseThrow(() -> new MedShopException.NotFoundException("User not found"));

        long linkedCount = authProviderRepo.countByTenantIdAndUserId(tenantId, userId);
        boolean hasPassword = user.getPasswordHash() != null;

        // Ensure at least one login method remains!
        if (!hasPassword && linkedCount <= 1) {
            throw new MedShopException.ConflictException(
                    "Cannot unlink provider. You must set a password or link another provider first.");
        }

        authProviderRepo.deleteByTenantIdAndUserIdAndProvider(tenantId, userId, provider.toUpperCase());
    }

    @Transactional(readOnly = true)
    public StoreUserProfileDto getProfile(String tenantId, String userId) {
        StoreUser user = userRepo.findByTenantIdAndId(tenantId, userId)
                .orElseThrow(() -> new MedShopException.NotFoundException("User not found"));
        Tenant tenant = tenantRepo.findById(tenantId)
                .orElseThrow(() -> new MedShopException.NotFoundException("Tenant not found"));

        List<String> providers = authProviderRepo.findByTenantIdAndUserId(tenantId, userId)
                .stream().map(p -> p.getProvider()).toList();

        TenantSummaryDto tenantSummary = new TenantSummaryDto(
                tenant.getId(), tenant.getShopName(), tenant.getGstin(), tenant.getStateCode(),
                tenant.getAddress(), tenant.getPhone(), tenant.getEmail(), tenant.isActive(),
                tenant.isCompositionDealer()
        );

        return new StoreUserProfileDto(
                user.getId(), user.getTenantId(), user.getUsername(), user.getEmail(), user.getPhone(),
                user.getRole(), user.getFirstName(), user.getMiddleName(), user.getLastName(),
                user.getProfilePic(), user.isActive(), tenantSummary, providers
        );
    }

    private void validateShopTaxRegistration(String gstin, String stateCode) {
        if (!ValidationPatterns.isValidStateCode(stateCode)) {
            throw new MedShopException.BadRequestException("Invalid 2-digit Indian GST state code: " + stateCode);
        }
        if (gstin != null && !gstin.isBlank()) {
            String cleanGstin = gstin.trim().toUpperCase();
            if (!ValidationPatterns.isValidGstin(cleanGstin)) {
                throw new MedShopException.BadRequestException("Invalid GSTIN format: " + cleanGstin);
            }
            if (!cleanGstin.startsWith(stateCode.trim())) {
                throw new MedShopException.BadRequestException(
                        "GSTIN state prefix (" + cleanGstin.substring(0, 2) + ") must match state code (" + stateCode + ")");
            }
        }
    }

    private String generateUniqueUsername(String base) {
        String prefix = base.contains("@") ? base.substring(0, base.indexOf('@')) : base;
        prefix = prefix.replaceAll("[^a-zA-Z0-9_]", "").toLowerCase();
        if (prefix.isBlank()) prefix = "user";
        String candidate = prefix;
        int count = 1;
        while (userRepo.existsByUsername(candidate)) {
            candidate = prefix + count++;
        }
        return candidate;
    }

    private void checkLockout(String identifier, String ip) {
        Instant now = Instant.now();
        FailureRecord rec = failureTracker.get(identifier);
        if (rec != null && rec.lockedUntil != null && now.isBefore(rec.lockedUntil)) {
            long remainingMins = ChronoUnit.MINUTES.between(now, rec.lockedUntil) + 1;
            throw new MedShopException.UnauthorizedException("Account is temporarily locked. Try again in " + remainingMins + " minutes.");
        }
    }

    private void recordFailure(String identifier, String ip) {
        failureTracker.compute(identifier, (k, existing) -> {
            int attempts = existing != null ? existing.attempts + 1 : 1;
            Instant lock = null;
            if (attempts >= maxLoginAttempts) {
                lock = Instant.now().plus(lockoutDurationMinutes, ChronoUnit.MINUTES);
                log.warn("Account {} locked for {} minutes due to {} repeated failures", identifier, lockoutDurationMinutes, attempts);
            }
            return new FailureRecord(attempts, lock);
        });
    }

    private void clearFailure(String identifier, String ip) {
        failureTracker.remove(identifier);
    }
}

