package com.pet.vetalya.auth.oauth2;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

import java.util.Map;
import java.util.Set;

/**
 * Jackson mixin for OAuth2AuthorizationRequest serialization/deserialization.
 * OAuth2AuthorizationRequest has no default constructor, so we need this.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class OAuth2AuthorizationRequestMixin {

    @JsonCreator
    public static OAuth2AuthorizationRequest create(
            @JsonProperty("authorizationUri") String authorizationUri,
            @JsonProperty("clientId") String clientId,
            @JsonProperty("redirectUri") String redirectUri,
            @JsonProperty("scopes") Set<String> scopes,
            @JsonProperty("state") String state,
            @JsonProperty("additionalParameters") Map<String, Object> additionalParameters,
            @JsonProperty("authorizationRequestUri") String authorizationRequestUri,
            @JsonProperty("attributes") Map<String, Object> attributes,
            @JsonProperty("authorizationGrantType") String grantType) {

        return OAuth2AuthorizationRequest.authorizationCode()
                .authorizationUri(authorizationUri)
                .clientId(clientId)
                .redirectUri(redirectUri)
                .scopes(scopes)
                .state(state)
                .additionalParameters(additionalParameters != null ? additionalParameters : Map.of())
                .attributes(a -> { if (attributes != null) a.putAll(attributes); })
                .build();
    }
}
