package com.pet.vetalya.medshop.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pet.vetalya.medshop.auth.dto.AuthDtos.*;
import com.pet.vetalya.medshop.auth.entity.AuthExchangeCode;
import com.pet.vetalya.medshop.auth.entity.StoreAuthProvider;
import com.pet.vetalya.medshop.auth.entity.StoreRefreshToken;
import com.pet.vetalya.medshop.auth.repository.AuthExchangeCodeRepository;
import com.pet.vetalya.medshop.auth.repository.StoreAuthProviderRepository;
import com.pet.vetalya.medshop.auth.repository.StoreRefreshTokenRepository;
import com.pet.vetalya.medshop.auth.service.StoreAuthService;
import com.pet.vetalya.medshop.auth.service.StoreTokenService;
import com.pet.vetalya.medshop.common.MedShopException;
import com.pet.vetalya.medshop.security.StoreUserStatusCache;
import com.pet.vetalya.medshop.tenant.entity.Tenant;
import com.pet.vetalya.medshop.tenant.repository.TenantRepository;
import com.pet.vetalya.medshop.user.entity.StoreUser;
import com.pet.vetalya.medshop.user.repository.StoreUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtEncoder;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StoreAuthAndRefreshRotationTest {

    @Mock private StoreUserRepository userRepo;
    @Mock private TenantRepository tenantRepo;
    @Mock private StoreAuthProviderRepository authProviderRepo;
    @Mock private StoreRefreshTokenRepository refreshTokenRepo;
    @Mock private AuthExchangeCodeRepository exchangeCodeRepo;
    @Mock private JwtEncoder storeJwtEncoder;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private StoreUserStatusCache userStatusCache;

    private StoreTokenService tokenService;
    private StoreAuthService authService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        tokenService = new StoreTokenService(
                storeJwtEncoder, refreshTokenRepo, userRepo, objectMapper, "test-secret-key-for-onboarding-token"
        );

        authService = new StoreAuthService(
                userRepo, tenantRepo, authProviderRepo,
                exchangeCodeRepo, tokenService, passwordEncoder, userStatusCache, objectMapper
        );
    }

    @Test
    @DisplayName("Valid refresh token rotates: old token marked used, new tokens issued")
    void testRefreshTokenRotationSuccess() {
        String rawToken = "raw-refresh-token-123";
        UUID familyId = UUID.randomUUID();

        StoreRefreshToken oldToken = new StoreRefreshToken();
        oldToken.setId(UUID.randomUUID().toString());
        oldToken.setTenantId("tenant-1");
        oldToken.setUserId("user-1");
        oldToken.setFamilyId(familyId);
        oldToken.setClientType("web");
        oldToken.setExpiresAt(Instant.now().plus(7, ChronoUnit.DAYS));
        oldToken.setRevoked(false);
        oldToken.setUsedAt(null);

        when(refreshTokenRepo.findByTokenHash(anyString())).thenReturn(Optional.of(oldToken));

        StoreUser user = new StoreUser();
        user.setId("user-1");
        user.setTenantId("tenant-1");
        user.setRole("OWNER");
        user.setActive(true);
        user.setTokenVersion(1);
        when(userRepo.findByTenantIdAndId("tenant-1", "user-1")).thenReturn(Optional.of(user));

        Jwt mockJwt = mock(Jwt.class);
        when(mockJwt.getTokenValue()).thenReturn("new-access-jwt");
        when(storeJwtEncoder.encode(any())).thenReturn(mockJwt);

        TokenResponse response = tokenService.rotateRefreshToken(rawToken, "web", "device-1", "127.0.0.1");

        assertThat(response.accessToken()).isEqualTo("new-access-jwt");
        assertThat(response.refreshToken()).isNotBlank();
        assertThat(oldToken.getUsedAt()).isNotNull();
    }

    @Test
    @DisplayName("Family reuse detection: reusing an already-used token revokes the entire token family")
    void testFamilyReuseDetectionRevokesFamily() {
        String rawToken = "reused-refresh-token";
        UUID familyId = UUID.randomUUID();

        StoreRefreshToken alreadyUsedToken = new StoreRefreshToken();
        alreadyUsedToken.setId(UUID.randomUUID().toString());
        alreadyUsedToken.setTenantId("tenant-1");
        alreadyUsedToken.setUserId("user-1");
        alreadyUsedToken.setFamilyId(familyId);
        alreadyUsedToken.setUsedAt(Instant.now().minus(1, ChronoUnit.HOURS)); // ALREADY USED!
        alreadyUsedToken.setRevoked(false);
        alreadyUsedToken.setExpiresAt(Instant.now().plus(7, ChronoUnit.DAYS));

        when(refreshTokenRepo.findByTokenHash(anyString())).thenReturn(Optional.of(alreadyUsedToken));

        assertThatThrownBy(() -> tokenService.rotateRefreshToken(rawToken, "web", "device-1", "127.0.0.1"))
                .isInstanceOf(MedShopException.UnauthorizedException.class)
                .hasMessageContaining("reuse detected");

        verify(refreshTokenRepo).revokeFamily(familyId);
    }

    @Test
    @DisplayName("Single-use exchange code: expired code throws 401 Unauthorized")
    void testExpiredExchangeCodeRejected() {
        AuthExchangeCode code = new AuthExchangeCode();
        code.setCodeHash("hash-xyz");
        code.setExpiresAt(Instant.now().minus(10, ChronoUnit.SECONDS)); // EXPIRED
        code.setUsedAt(null);

        when(exchangeCodeRepo.findByCodeHash(anyString())).thenReturn(Optional.of(code));

        assertThatThrownBy(() -> authService.exchangeCode("raw-code-xyz"))
                .isInstanceOf(MedShopException.UnauthorizedException.class)
                .hasMessageContaining("expired or already used");
    }

    @Test
    @DisplayName("Register shop via social onboarding creates Tenant, Owner, and AuthProvider in one atomic flow")
    void testRegisterShopSocialFlow() {
        String email = "doctor@gmail.com";
        String onboardingToken = tokenService.createOnboardingToken("GOOGLE", "google-sub-12345", email, true, "Dr. Smith");

        when(userRepo.existsByEmail(email)).thenReturn(false);
        when(userRepo.existsByUsername("drsmith")).thenReturn(false);
        when(userRepo.existsByPhone("9876543210")).thenReturn(false);

        Jwt mockJwt = mock(Jwt.class);
        when(mockJwt.getTokenValue()).thenReturn("access-token-jwt");
        when(storeJwtEncoder.encode(any())).thenReturn(mockJwt);

        when(tenantRepo.save(any(Tenant.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userRepo.save(any(StoreUser.class))).thenAnswer(inv -> inv.getArgument(0));

        RegisterShopSocialRequest request = new RegisterShopSocialRequest(
                onboardingToken,
                "Dr. Smith Pharmacy",
                "27AAAAA0000A1Z5",
                "27",
                "Pune, Maharashtra",
                "9876543210",
                false,
                "drsmith"
        );

        TokenResponse response = authService.registerShopSocial(request, "web", "device-1", "127.0.0.1");

        assertThat(response).isNotNull();
        assertThat(response.accessToken()).isEqualTo("access-token-jwt");
        verify(tenantRepo).save(any(Tenant.class));
        verify(userRepo).save(any(StoreUser.class));
        verify(authProviderRepo).save(any(StoreAuthProvider.class));
    }
}
