package com.pet.vetalya.logging;

import ch.qos.logback.classic.pattern.MessageConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Logback converter that acts as a logging-layer safety net, redacting JWTs,
 * Bearer tokens, numeric OTPs, passwords, phones, and sensitive keys from all formatted log messages.
 */
public class SensitiveDataMaskingConverter extends MessageConverter {

    private static final Pattern BEARER_JWT_PATTERN = Pattern.compile(
            "(?i)bearer\\s+[a-zA-Z0-9-_=]+\\.[a-zA-Z0-9-_=]+\\.?[a-zA-Z0-9-_.+/=]*"
    );

    private static final Pattern RAW_JWT_PATTERN = Pattern.compile(
            "\\beyJ[a-zA-Z0-9-_=]+\\.[a-zA-Z0-9-_=]+\\.[a-zA-Z0-9-_.+/=]*\\b"
    );

    private static final Pattern OTP_PATTERN = Pattern.compile(
            "(?i)\\b(otp|code|pin|verification[-_]?code)\\b\\s*([:=])\\s*[\"']?(\\d{4,8})[\"']?"
    );

    private static final Pattern SECRET_PATTERN = Pattern.compile(
            "(?i)\\b(email[-_]?verification[-_]?token|id[-_]?token|access[-_]?token|password|client[-_]?secret|private[-_]?key|p8[-_]?key|token[-_]?hash|refresh[-_]?token)\\b\\s*([:=])\\s*[\"']?([^\\s\"',;]+)[\"']?"
    );

    private static final Pattern AUTH_HEADER_PATTERN = Pattern.compile(
            "(?i)\\bauthorization\\b\\s*([:=])\\s*[\"']?Bearer\\s+[^\\s\"',;]+[\"']?"
    );

    private static final Pattern PHONE_FIELD_PATTERN = Pattern.compile(
            "(?i)\\b(phone|mobile)\\b\\s*([:=])\\s*[\"']?(\\+?\\d{10,13})[\"']?"
    );

    private static final Pattern STANDALONE_PHONE_PATTERN = Pattern.compile(
            "\\+91\\d{10}"
    );

    @Override
    public String convert(ILoggingEvent event) {
        String in = super.convert(event);
        if (in == null || in.isEmpty()) {
            return in;
        }
        return mask(in);
    }

    public static String mask(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        // 1. Mask Bearer JWTs
        String text = BEARER_JWT_PATTERN.matcher(input).replaceAll("Bearer [REDACTED_JWT]");

        // 2. Mask Raw JWTs
        text = RAW_JWT_PATTERN.matcher(text).replaceAll("[REDACTED_JWT]");

        // 3. Mask OTPs
        Matcher otpMatcher = OTP_PATTERN.matcher(text);
        if (otpMatcher.find()) {
            text = otpMatcher.replaceAll("$1$2******");
        }

        // 4. Mask Authorization headers
        text = AUTH_HEADER_PATTERN.matcher(text).replaceAll("authorization$1Bearer [REDACTED]");

        // 5. Mask Passwords and Secrets
        Matcher secretMatcher = SECRET_PATTERN.matcher(text);
        if (secretMatcher.find()) {
            text = secretMatcher.replaceAll("$1$2[REDACTED]");
        }

        // 6. Mask phone numbers
        Matcher phoneMatcher = PHONE_FIELD_PATTERN.matcher(text);
        if (phoneMatcher.find()) {
            text = phoneMatcher.replaceAll(mr -> mr.group(1) + mr.group(2) + MaskingUtil.maskPhone(mr.group(3)));
        }
        Matcher standalonePhoneMatcher = STANDALONE_PHONE_PATTERN.matcher(text);
        if (standalonePhoneMatcher.find()) {
            text = standalonePhoneMatcher.replaceAll(mr -> MaskingUtil.maskPhone(mr.group()));
        }

        return text;
    }
}
