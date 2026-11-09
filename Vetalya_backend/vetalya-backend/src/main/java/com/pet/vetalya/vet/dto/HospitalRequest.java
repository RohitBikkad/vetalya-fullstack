package com.pet.vetalya.vet.dto;

import jakarta.validation.constraints.*;
import java.time.LocalTime;

/**
 * Request DTO for creating / updating a hospital.
 */
public record HospitalRequest(
        @NotBlank(message = "Hospital name is required")
        @Size(max = 200, message = "Name must be at most 200 characters")
        String name,

        @NotBlank(message = "Address line 1 is required")
        @Size(max = 255, message = "Address1 must be at most 255 characters")
        String address1,

        @Size(max = 255, message = "Address2 must be at most 255 characters")
        String address2,

        @NotBlank(message = "City is required")
        @Size(max = 100, message = "City must be at most 100 characters")
        String city,

        @NotBlank(message = "State is required")
        @Size(max = 100, message = "State must be at most 100 characters")
        String state,

        @NotBlank(message = "Pincode is required")
        @Pattern(regexp = "^\\d{6}$", message = "Pincode must be exactly 6 digits")
        String pincode,

        @Size(max = 100, message = "Country must be at most 100 characters")
        String country,

        @NotBlank(message = "Mobile number is required")
        @Pattern(regexp = "^\\+[1-9]\\d{1,14}$", message = "Mobile must be in E.164 format (e.g. +919876543210)")
        String mobile,

        @Email(message = "Invalid email format")
        @Size(max = 255, message = "Email must be at most 255 characters")
        String email,

        @Size(min = 15, max = 15, message = "GST number must be exactly 15 characters")
        String gstNumber,

        boolean is24x7,

        LocalTime openTime,

        LocalTime closeTime
) {}
