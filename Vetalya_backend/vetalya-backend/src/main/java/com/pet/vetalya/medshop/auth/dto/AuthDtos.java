package com.pet.vetalya.medshop.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public final class AuthDtos {

    private AuthDtos() {}

    public record LoginRequest(
            @NotBlank(message = "Identifier (username, email or phone) is required")
            String identifier,

            @NotBlank(message = "Password is required")
            @Size(min = 10, message = "Password must be at least 10 characters")
            String password
    ) {}

    public record TokenResponse(
            String accessToken,
            String refreshToken,
            String tokenType,
            long expiresIn,
            String status,
            String onboardingToken
    ) {
        public static TokenResponse of(String accessToken, String refreshToken, long expiresIn) {
            return new TokenResponse(accessToken, refreshToken, "Bearer", expiresIn, "SUCCESS", null);
        }

        public static TokenResponse onboarding(String onboardingToken) {
            return new TokenResponse(null, null, null, 0, "ONBOARDING_REQUIRED", onboardingToken);
        }
    }

    public record RegisterShopRequest(
            @NotBlank(message = "Shop name is required")
            String shopName,
            String gstin,
            @NotBlank(message = "2-digit GST state code is required")
            String stateCode,
            @NotBlank(message = "Shop address is required")
            String address,
            @NotBlank(message = "Phone number is required")
            String phone,
            @NotBlank(message = "Email is required")
            String email,
            Boolean isCompositionDealer,
            String username,
            @NotBlank(message = "First name is required")
            String ownerFirstName,
            @NotBlank(message = "Last name is required")
            String ownerLastName,
            @NotBlank(message = "Password is required")
            @Size(min = 10, message = "Password must be at least 10 characters")
            String password
    ) {}

    public record RegisterShopSocialRequest(
            @NotBlank(message = "Onboarding token is required")
            String onboardingToken,
            @NotBlank(message = "Shop name is required")
            String shopName,
            String gstin,
            @NotBlank(message = "2-digit GST state code is required")
            String stateCode,
            @NotBlank(message = "Shop address is required")
            String address,
            @NotBlank(message = "Phone number is required")
            String phone,
            Boolean isCompositionDealer,
            String username
    ) {}

    public record ExchangeCodeRequest(
            @NotBlank(message = "One-time exchange code is required")
            String code
    ) {}

    public record SocialAuthRequest(
            @NotBlank(message = "idToken is required")
            String idToken,
            String name,
            String deviceId
    ) {}

    public record LinkProviderRequest(
            @NotBlank(message = "idToken is required")
            String idToken
    ) {}

    public record ChangePasswordRequest(
            String currentPassword,
            @NotBlank(message = "New password is required")
            @Size(min = 10, message = "Password must be at least 10 characters")
            String newPassword
    ) {}

    public record StoreUserProfileDto(
            String id,
            String tenantId,
            String username,
            String email,
            String phone,
            String role,
            String firstName,
            String middleName,
            String lastName,
            String profilePic,
            boolean active,
            TenantSummaryDto tenant,
            List<String> linkedProviders
    ) {}

    public record TenantSummaryDto(
            String id,
            String shopName,
            String gstin,
            String stateCode,
            String address,
            String phone,
            String email,
            boolean active,
            boolean compositionDealer
    ) {}
}

