package com.pet.vetalya.auth;

import com.pet.vetalya.common.GlobalExceptionHandler.BadRequestException;
import java.util.Locale;

/** Client transport selects token delivery, never account roles or data access. */
public enum ClientPlatform {
    WEB, ANDROID, IOS;
    public static ClientPlatform parse(String value) {
        if (value == null || value.isBlank()) return WEB;
        try { return valueOf(value.trim().toUpperCase(Locale.ROOT)); }
        catch (IllegalArgumentException ex) { throw new BadRequestException("clientType must be web, android or ios"); }
    }
    public String value() { return name().toLowerCase(Locale.ROOT); }
    public boolean isWeb() { return this == WEB; }
}
