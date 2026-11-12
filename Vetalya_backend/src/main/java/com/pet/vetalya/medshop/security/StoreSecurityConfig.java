package com.pet.vetalya.medshop.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.client.endpoint.RestClientAuthorizationCodeTokenResponseClient;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Dedicated SecurityFilterChain for all MedShop module endpoints under /api/store/**.
 * @Order(1) ensures it precedes the Veterinary API chain (@Order(2)), preventing any cross-talk.
 * Completely stateless, dedicated RS256 JWT validation, and custom OAuth2 login endpoints.
 */
@Configuration
@EnableMethodSecurity(prePostEnabled = true)
public class StoreSecurityConfig {

    private final StoreCookieAuthRequestRepo cookieAuthRequestRepo;
    private final StoreOAuth2SuccessHandler oauth2SuccessHandler;
    private final StoreOAuth2FailureHandler oauth2FailureHandler;
    private final StoreAppleClientSecretGenerator appleSecretGenerator;

    @Value("${app.cors.allowed-origin:http://localhost:3000}")
    private String allowedOrigin;

    public StoreSecurityConfig(
            StoreCookieAuthRequestRepo cookieAuthRequestRepo,
            StoreOAuth2SuccessHandler oauth2SuccessHandler,
            StoreOAuth2FailureHandler oauth2FailureHandler,
            StoreAppleClientSecretGenerator appleSecretGenerator) {
        this.cookieAuthRequestRepo = cookieAuthRequestRepo;
        this.oauth2SuccessHandler = oauth2SuccessHandler;
        this.oauth2FailureHandler = oauth2FailureHandler;
        this.appleSecretGenerator = appleSecretGenerator;
    }

    @Bean
    public StoreTokenFilter storeTokenFilter(StoreUserStatusCache userStatusCache) {
        return new StoreTokenFilter(userStatusCache);
    }

    @Bean
    public org.springframework.boot.web.servlet.FilterRegistrationBean<StoreTokenFilter> storeTokenFilterRegistration(StoreTokenFilter filter) {
        org.springframework.boot.web.servlet.FilterRegistrationBean<StoreTokenFilter> registration =
                new org.springframework.boot.web.servlet.FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    @Order(1)
    public SecurityFilterChain storeSecurityFilterChain(
            HttpSecurity http,
            @Qualifier("storeJwtDecoder") JwtDecoder storeJwtDecoder,
            StoreTokenFilter storeTokenFilter,
            @Autowired(required = false) ClientRegistrationRepository clientRegistrationRepo) throws Exception {

        // Custom token response client injecting dynamic ES256 client_secret for Apple
        RestClientAuthorizationCodeTokenResponseClient tokenResponseClient =
                new RestClientAuthorizationCodeTokenResponseClient();
        tokenResponseClient.addParametersConverter(grantRequest -> {
            String regId = grantRequest.getClientRegistration().getRegistrationId();
            if ("apple-store".equals(regId) || "apple".equals(regId)) {
                MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
                params.add("client_secret", appleSecretGenerator.generateClientSecret());
                return params;
            }
            return new LinkedMultiValueMap<>();
        });

        http
            .securityMatcher("/api/store/**")
            .cors(cors -> cors.configurationSource(storeCorsConfigurationSource()))
            .csrf(csrf -> csrf
                // Disable CSRF for stateless Bearer endpoints; ignore Apple callback form_post
                .ignoringRequestMatchers("/api/store/login/oauth2/code/*")
                .disable()
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .authorizeHttpRequests(auth -> auth
                // Public authentication and onboarding endpoints
                .requestMatchers(
                    "/api/store/auth/login",
                    "/api/store/auth/refresh",
                    "/api/store/auth/register-shop",
                    "/api/store/auth/register-shop/social",
                    "/api/store/auth/exchange",
                    "/api/store/auth/google",
                    "/api/store/auth/apple",
                    "/api/store/oauth2/authorization/**",
                    "/api/store/login/oauth2/code/**"
                ).permitAll()
                // All other business endpoints require authentication
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt
                    .decoder(storeJwtDecoder)
                    .jwtAuthenticationConverter(storeJwtAuthenticationConverter())
                )
            );

        if (clientRegistrationRepo != null) {
            http.oauth2Login(oauth2 -> oauth2
                .authorizationEndpoint(auth -> auth
                    .baseUri("/api/store/oauth2/authorization")
                    .authorizationRequestRepository(cookieAuthRequestRepo)
                    .authorizationRequestResolver(
                        new StoreAppleAuthorizationResolver(clientRegistrationRepo, "/api/store/oauth2/authorization")
                    )
                )
                .redirectionEndpoint(redr -> redr
                    .baseUri("/api/store/login/oauth2/code/*")
                )
                .tokenEndpoint(token -> token
                    .accessTokenResponseClient(tokenResponseClient)
                )
                .successHandler(oauth2SuccessHandler)
                .failureHandler(oauth2FailureHandler)
            );
        }

        // Add user status & tenant context filter after token authentication
        http.addFilterAfter(storeTokenFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    private JwtAuthenticationConverter storeJwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authoritiesConverter = new JwtGrantedAuthoritiesConverter();
        authoritiesConverter.setAuthoritiesClaimName("roles");
        authoritiesConverter.setAuthorityPrefix(""); // Roles already carry "ROLE_" prefix

        JwtAuthenticationConverter jwtConverter = new JwtAuthenticationConverter();
        jwtConverter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);
        return jwtConverter;
    }

    private CorsConfigurationSource storeCorsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(new com.pet.vetalya.config.AppProperties.Cors(allowedOrigin).allowedOrigins());
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Idempotency-Key", "X-Client-Type", "X-Request-Id"));
        config.setExposedHeaders(List.of("X-Request-Id"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/store/**", config);
        return source;
    }
}

