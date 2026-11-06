package com.pet.vetalya.dto;

public record NativeLoginRequest(
        String idToken,
        String role,
        String name,       // Optional: Apple sends name only on first auth from the device
        String deviceId
) {}
