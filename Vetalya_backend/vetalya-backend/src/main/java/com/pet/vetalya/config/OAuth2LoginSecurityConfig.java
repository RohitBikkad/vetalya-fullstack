package com.pet.vetalya.config;

import com.pet.vetalya.auth.apple.AppleClientSecretGenerator;
import com.pet.vetalya.auth.mapper.AppleUserMapper;
import com.pet.vetalya.auth.mapper.GoogleUserMapper;
import com.pet.vetalya.auth.oauth2.*;
import com.pet.vetalya.auth.service.SocialLoginService;
import com.pet.vetalya.repository.OneTimeCodeRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.client.endpoint.RestClientAuthorizationCodeTokenResponseClient;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;

/**
 * SecurityFilterChain for OAuth2 login endpoints: /oauth2/** and
 * /login/oauth2/**.
 * Handles the server-side redirect flow for Google and Apple Sign In.
 */
@Configuration
@EnableWebSecurity
public class OAuth2LoginSecurityConfig {

    @Value("${COOKIE_SIGNING_SECRET:vetalya-dev-cookie-secret-change-in-prod}")
    private String cookieSigningSecret;

    @Bean
    public CookieAuthorizationRequestRepository cookieAuthorizationRequestRepository() {
        return new CookieAuthorizationRequestRepository(cookieSigningSecret);
    }

    @Bean
    @Order(1)
    public SecurityFilterChain oauth2LoginFilterChain(
            HttpSecurity http,
            ClientRegistrationRepository clientRegistrationRepo,
            CookieAuthorizationRequestRepository cookieAuthRequestRepo,
            GoogleUserMapper googleMapper,
            AppleUserMapper appleMapper,
            SocialLoginService socialLoginService,
            AppProperties appProperties,
            OneTimeCodeRepository oneTimeCodeRepo,
            AppleClientSecretGenerator appleSecretGenerator) throws Exception {

        // Custom token response client that injects Apple's dynamic client_secret
        RestClientAuthorizationCodeTokenResponseClient tokenResponseClient = new RestClientAuthorizationCodeTokenResponseClient();
        tokenResponseClient.addParametersConverter(grantRequest -> {
            String regId = grantRequest.getClientRegistration().getRegistrationId();
            if ("apple".equals(regId)) {
                org.springframework.util.MultiValueMap<String, String> params = new org.springframework.util.LinkedMultiValueMap<>();
                params.add("client_secret", appleSecretGenerator.generateClientSecret());
                return params;
            }
            return new org.springframework.util.LinkedMultiValueMap<>();
        });

        // Success and failure handlers
        OAuth2LoginSuccessHandler successHandler = new OAuth2LoginSuccessHandler(
                cookieAuthRequestRepo, googleMapper, appleMapper,
                socialLoginService, appProperties, oneTimeCodeRepo);

        OAuth2LoginFailureHandler failureHandler = new OAuth2LoginFailureHandler(appProperties, cookieAuthRequestRepo);

        // Authorization request resolver (adds response_mode=form_post for Apple)
        AppleOAuth2AuthorizationRequestResolver requestResolver = new AppleOAuth2AuthorizationRequestResolver(
                clientRegistrationRepo);

        http
                .securityMatcher("/oauth2/**", "/login/oauth2/**")
                .csrf(csrf -> csrf
                        // Disable CSRF for the OAuth2 callback — state parameter protects against CSRF.
                        // Required for Apple's cross-site form_post.
                        .ignoringRequestMatchers("/login/oauth2/code/*"))
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll())
                .oauth2Login(oauth2 -> oauth2
                        .authorizationEndpoint(authorization -> authorization
                                .authorizationRequestRepository(cookieAuthRequestRepo)
                                .authorizationRequestResolver(requestResolver))
                        .tokenEndpoint(token -> token
                                .accessTokenResponseClient(tokenResponseClient))
                        .successHandler(successHandler)
                        .failureHandler(failureHandler));

        return http.build();
    }
}
