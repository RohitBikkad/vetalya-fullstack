package com.pet.vetalya.medshop.customer.dto;

import jakarta.validation.constraints.NotBlank;

public final class CustomerDtos {

    private CustomerDtos() {}

    public record CustomerDto(
            String id,
            String name,
            String phone,
            String gstin,
            String address,
            String stateCode
    ) {}

    public record CreateCustomerRequest(
            @NotBlank(message = "Customer name is required")
            String name,
            @NotBlank(message = "Phone number is required")
            String phone,
            String gstin,
            String address,
            String stateCode
    ) {}

    public record UpdateCustomerRequest(
            @NotBlank(message = "Customer name is required")
            String name,
            @NotBlank(message = "Phone number is required")
            String phone,
            String gstin,
            String address,
            String stateCode
    ) {}
}

