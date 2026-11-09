package com.pet.vetalya.medshop.common;

import java.util.Set;
import java.util.regex.Pattern;

public final class ValidationPatterns {

    private ValidationPatterns() {}

    // Indian GSTIN format: 2 digits state code + 10 alphanumeric PAN + 1 entity code + 'Z' + 1 checksum
    public static final String GSTIN_REGEX = "^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}$";
    public static final Pattern GSTIN_PATTERN = Pattern.compile(GSTIN_REGEX);

    // 2-digit Indian State code (01 to 38, 97, 99)
    public static final String STATE_CODE_REGEX = "^(0[1-9]|[1-2][0-9]|3[0-8]|97|99)$";
    public static final Pattern STATE_CODE_PATTERN = Pattern.compile(STATE_CODE_REGEX);

    // Phone pattern (e.g. +919876543210 or 9876543210)
    public static final String PHONE_REGEX = "^(\\+91)?[6-9]\\d{9}$";
    public static final Pattern PHONE_PATTERN = Pattern.compile(PHONE_REGEX);

    // Standard GST rates in India (0%, 0.1%, 0.25%, 3%, 5%, 12%, 18%, 28%)
    public static final Set<Double> DEFAULT_ALLOWED_GST_RATES = Set.of(0.0, 0.1, 0.25, 3.0, 5.0, 12.0, 18.0, 28.0);

    public static boolean isValidGstin(String gstin) {
        return gstin != null && GSTIN_PATTERN.matcher(gstin.trim()).matches();
    }

    public static boolean isValidStateCode(String stateCode) {
        return stateCode != null && STATE_CODE_PATTERN.matcher(stateCode.trim()).matches();
    }

    public static boolean isValidPhone(String phone) {
        return phone != null && PHONE_PATTERN.matcher(phone.trim()).matches();
    }
}

