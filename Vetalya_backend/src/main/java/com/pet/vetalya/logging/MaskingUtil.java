package com.pet.vetalya.logging;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility for masking sensitive fields (phone, email, credentials) before logging or external reporting.
 */
public final class MaskingUtil {

    private static final Pattern IN_PHONE_PATTERN = Pattern.compile("^(\\+91)(\\d{6})(\\d{4})$");
    private static final Pattern GENERIC_PHONE_PATTERN = Pattern.compile("^(\\+?\\d{1,4})?(\\d{2,})(\\d{4})$");

    private MaskingUtil() {}

    /**
     * Masks phone number to preserve country code and last 4 digits with 6 X's in the middle.
     * Example: "+919876543210" -> "+91XXXXXX3210", "9876543210" -> "XXXXXX3210"
     */
    public static String maskPhone(String phone) {
        if (phone == null || phone.isBlank()) {
            return phone;
        }
        String cleaned = phone.trim();
        if (cleaned.length() <= 4) {
            return cleaned;
        }
        Matcher inMatcher = IN_PHONE_PATTERN.matcher(cleaned);
        if (inMatcher.matches()) {
            return "+91XXXXXX" + inMatcher.group(3);
        }
        if (cleaned.length() == 10 && cleaned.matches("^\\d{10}$")) {
            return cleaned.substring(0, 2) + "XXXX" + cleaned.substring(6);
        }
        Matcher matcher = GENERIC_PHONE_PATTERN.matcher(cleaned);
        if (matcher.matches()) {
            String prefix = matcher.group(1) != null ? matcher.group(1) : "";
            String middle = matcher.group(2);
            String suffix = matcher.group(3);
            return prefix + "X".repeat(middle.length()) + suffix;
        }
        if (cleaned.length() > 4) {
            return "X".repeat(cleaned.length() - 4) + cleaned.substring(cleaned.length() - 4);
        }
        return "XXXX";
    }

    /**
     * Masks email keeping the first character of the username and the domain prefix.
     * Example: "rajesh@gmail.com" -> "r***@g***.com"
     */
    public static String maskEmail(String email) {
        if (email == null || email.isBlank()) {
            return email;
        }
        String cleaned = email.trim();
        int atIndex = cleaned.indexOf('@');
        if (atIndex <= 0 || atIndex == cleaned.length() - 1) {
            return "***";
        }
        String username = cleaned.substring(0, atIndex);
        String domain = cleaned.substring(atIndex + 1);

        String maskedUser = username.charAt(0) + "***";
        int dotIndex = domain.indexOf('.');
        String maskedDomain;
        if (dotIndex > 0) {
            String domainName = domain.substring(0, dotIndex);
            String tld = domain.substring(dotIndex);
            maskedDomain = domainName.charAt(0) + "***" + tld;
        } else {
            maskedDomain = domain.charAt(0) + "***";
        }

        return maskedUser + "@" + maskedDomain;
    }
}
