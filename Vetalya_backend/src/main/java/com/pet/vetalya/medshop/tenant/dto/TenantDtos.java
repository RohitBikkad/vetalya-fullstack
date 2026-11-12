package com.pet.vetalya.medshop.tenant.dto;

import jakarta.validation.constraints.NotBlank;

public final class TenantDtos {

    private TenantDtos() {}

    public record TenantResponse(
            String id,
            String shopName,
            String gstin,
            String stateCode,
            String address,
            String phone,
            String email,
            String plan,
            boolean active,
            boolean compositionDealer
    ) {}

    public record UpdateTenantRequest(
            @NotBlank(message = "Shop name is required")
            String shopName,
            @NotBlank(message = "Address is required")
            String address,
            @NotBlank(message = "Phone number is required")
            String phone,
            @NotBlank(message = "Email is required")
            String email,
            String gstin,
            @NotBlank(message = "State code is required")
            String stateCode,
            Boolean isCompositionDealer
    ) {}
}

