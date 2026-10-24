package com.pet.vetalya.auth.mapper;

import com.pet.vetalya.dto.ProviderUserInfo;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Component;

@Component
public class GoogleUserMapper {

    public ProviderUserInfo map(OidcUser oidcUser) {
        return new ProviderUserInfo(
                "GOOGLE",
                oidcUser.getSubject(),
                oidcUser.getEmail(),
                Boolean.TRUE.equals(oidcUser.getEmailVerified()),
                oidcUser.getGivenName(),
                oidcUser.getFamilyName(),
                oidcUser.getPicture()
        );
    }
}
