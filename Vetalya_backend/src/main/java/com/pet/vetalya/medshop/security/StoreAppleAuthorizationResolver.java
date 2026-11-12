package com.pet.vetalya.medshop.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Custom resolver that adds response_mode=form_post when registrationId is "apple-store",
 * required by Apple when requesting name and email scopes.
 */
public class StoreAppleAuthorizationResolver implements OAuth2AuthorizationRequestResolver {

    private final DefaultOAuth2AuthorizationRequestResolver defaultResolver;

    public StoreAppleAuthorizationResolver(ClientRegistrationRepository repo, String authorizationRequestBaseUri) {
        this.defaultResolver = new DefaultOAuth2AuthorizationRequestResolver(repo, authorizationRequestBaseUri);
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request) {
        OAuth2AuthorizationRequest req = defaultResolver.resolve(request);
        return customize(req);
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request, String clientRegistrationId) {
        OAuth2AuthorizationRequest req = defaultResolver.resolve(request, clientRegistrationId);
        return customize(req);
    }

    private OAuth2AuthorizationRequest customize(OAuth2AuthorizationRequest req) {
        if (req == null) {
            return null;
        }
        String regId = (String) req.getAttributes().get("registration_id");
        if ("apple-store".equals(regId) || "apple".equals(regId)) {
            Map<String, Object> additional = new LinkedHashMap<>(req.getAdditionalParameters());
            additional.put("response_mode", "form_post");
            return OAuth2AuthorizationRequest.from(req)
                    .additionalParameters(additional)
                    .build();
        }
        return req;
    }
}

