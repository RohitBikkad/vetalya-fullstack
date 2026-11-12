package com.pet.vetalya.dto;

public record RefreshRequest(
        String refreshToken,  // Mobile sends in body; web reads from cookie
        String deviceId
) {}
