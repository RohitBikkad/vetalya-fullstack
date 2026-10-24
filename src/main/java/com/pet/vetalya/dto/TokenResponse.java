package com.pet.vetalya.dto;

public record TokenResponse(
        String accessToken,
        String refreshToken,  // null for web (sent via cookie)
        String tokenType,
        int expiresIn,
        boolean newUser,
        boolean profileComplete
) {
    public TokenResponse(String accessToken, String refreshToken, int expiresIn,
                         boolean newUser, boolean profileComplete) {
        this(accessToken, refreshToken, "Bearer", expiresIn, newUser, profileComplete);
    }
}
