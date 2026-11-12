package com.pet.vetalya.auth.oauth2;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Custom resolver that adds response_mode=form_post for the Apple registration only.
 * Apple REQUIRES form_post when requesting name/email scopes.
 * Google uses the default redirect (GET) mode.
 */
public class AppleOAuth2AuthorizationRequestResolver implements OAuth2AuthorizationRequestResolver {

    private final DefaultOAuth2AuthorizationRequestResolver delegate;

    public AppleOAuth2AuthorizationRequestResolver(ClientRegistrationRepository repo) {
        this.delegate = new DefaultOAuth2AuthorizationRequestResolver(repo, "/oauth2/authorization");
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request) {
        OAuth2AuthorizationRequest req = delegate.resolve(request);
        return customize(req, request);
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request, String clientRegistrationId) {
        OAuth2AuthorizationRequest req = delegate.resolve(request, clientRegistrationId);
        return customize(req, request);
    }

    private OAuth2AuthorizationRequest customize(OAuth2AuthorizationRequest request,
                                                  HttpServletRequest servletRequest) {
        if (request == null) return null;

        // Detect Apple registration from the authorization URI
        if (request.getAuthorizationUri().contains("appleid.apple.com")) {
            Map<String, Object> additionalParams = new LinkedHashMap<>(request.getAdditionalParameters());
            additionalParams.put("response_mode", "form_post");

            return OAuth2AuthorizationRequest.from(request)
                    .additionalParameters(additionalParams)
                    .build();
        }

        return request;
    }
}
