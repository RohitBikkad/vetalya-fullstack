package com.pet.vetalya.auth.mapper;

import com.pet.vetalya.dto.ProviderUserInfo;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Component;

@Component
public class AppleUserMapper {

    /**
     * Maps Apple id_token claims to ProviderUserInfo.
     * Apple does NOT include name in the id_token — it arrives separately in the form_post "user" param
     * on first authorization only, and must be passed via firstName/lastName parameters.
     *
     * email_verified can arrive as boolean true or String "true" from Apple.
     */
    public ProviderUserInfo map(OidcUser oidcUser, String firstName, String lastName) {
        Object emailVerifiedRaw = oidcUser.getAttribute("email_verified");
        boolean emailVerified = parseEmailVerified(emailVerifiedRaw);

        return new ProviderUserInfo(
                "APPLE",
                oidcUser.getSubject(),
                oidcUser.getEmail(),
                emailVerified,
                firstName,
                lastName,
                null  // Apple does not provide a picture
        );
    }

    private boolean parseEmailVerified(Object value) {
        if (value instanceof Boolean b) return b;
        if (value instanceof String s) return "true".equalsIgnoreCase(s);
        return false;
    }
}
