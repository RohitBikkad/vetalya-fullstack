package com.pet.vetalya.dto;

/**
 * Provider-neutral user info extracted from any OAuth provider.
 * Each provider has a mapper that fills this record.
 */
public record ProviderUserInfo(
        String provider,      // GOOGLE or APPLE
        String sub,           // Provider's unique user ID
        String email,
        boolean emailVerified,
        String firstName,
        String lastName,
        String pictureUrl     // null for Apple
) {}
