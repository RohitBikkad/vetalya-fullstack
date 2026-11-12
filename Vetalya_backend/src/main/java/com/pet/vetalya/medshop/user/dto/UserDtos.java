package com.pet.vetalya.medshop.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class UserDtos {

    private UserDtos() {}

    public record StoreUserResponse(
            String id,
            String tenantId,
            String username,
            String email,
            String phone,
            String role,
            String firstName,
            String middleName,
            String lastName,
            String address,
            String profilePic,
            boolean active
    ) {}

    public record CreateStaffRequest(
            @NotBlank(message = "Email is required")
            String email,
            @NotBlank(message = "Phone number is required")
            String phone,
            @NotBlank(message = "First name is required")
            String firstName,
            String middleName,
            @NotBlank(message = "Last name is required")
            String lastName,
            @NotBlank(message = "Role is required (OWNER, PHARMACIST, CASHIER)")
            String role,
            String username,
            @Size(min = 10, message = "Password must be at least 10 characters")
            String initialPassword
    ) {}

    public record UpdateUserRequest(
            @NotBlank(message = "First name is required")
            String firstName,
            String middleName,
            @NotBlank(message = "Last name is required")
            String lastName,
            String address,
            String profilePic
    ) {}

    public record ChangeRoleRequest(
            @NotBlank(message = "Role is required (OWNER, PHARMACIST, CASHIER)")
            String role
    ) {}

    public record ChangeActiveRequest(
            boolean active
    ) {}

    public record PresignedUrlRequest(
            @NotBlank(message = "Content type is required (e.g. image/jpeg, image/png)")
            String contentType
    ) {}

    public record PresignedUrlResponse(
            String uploadUrl,
            String publicUrl,
            String key
    ) {}
}

