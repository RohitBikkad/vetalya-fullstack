package com.pet.vetalya.dto;

public record NativeLoginRequest(
        String idToken,
        String role,
        String name,       // Optional: Apple sends name only on first auth from the device
        String deviceId,
        String clientType // web (default), android or ios; never changes authorization
) {
    public NativeLoginRequest(String idToken, String role, String name, String deviceId) {
        this(idToken, role, name, deviceId, null);
    }
}
