package com.pet.vetalya.owner.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record OwnerProfileRequest(
        @NotBlank(message = "First name is required")
        @Size(max = 100, message = "First name must be at most 100 characters")
        String firstName,

        @NotBlank(message = "Last name is required")
        @Size(max = 100, message = "Last name must be at most 100 characters")
        String lastName,

        @Size(max = 255, message = "Address line 1 must be at most 255 characters")
        String address1,

        @Size(max = 255, message = "Address line 2 must be at most 255 characters")
        String address2,

        @Pattern(regexp = "^\\d{6}$", message = "Pincode must be exactly 6 digits")
        String pincode,

        @Size(max = 100, message = "City must be at most 100 characters")
        String city,

        @Size(max = 100, message = "State must be at most 100 characters")
        String state,

        @Size(max = 100, message = "Country must be at most 100 characters")
        String country
) {}
