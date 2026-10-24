package com.pet.vetalya.config;

import com.pet.vetalya.repository.UserAuthRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * SecurityFilterChain for /api/** — stateless JWT resource server.
 * CSRF disabled (Bearer token), CORS for web origin, token_version validation.
 */
@Configuration
public class ApiSecurityConfig {

    private final AppProperties appProperties;
    private final UserAuthRepository userAuthRepo;

    public ApiSecurityConfig(AppProperties appProperties, UserAuthRepository userAuthRepo) {
        this.appProperties = appProperties;
        this.userAuthRepo = userAuthRepo;
    }

    @Bean
    @Order(2)
    public SecurityFilterChain apiFilterChain(HttpSecurity http, JwtDecoder jwtDecoder) throws Exception {
        http
            .securityMatcher("/api/**")
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .authorizeHttpRequests(auth -> auth
                // Public auth and status endpoints
                .requestMatchers(
                    "/api/public/**",
                    "/api/auth/refresh",
                    "/api/auth/exchange",
                    "/api/auth/google",
                    "/api/auth/apple",
                    "/api/auth/logout"
                ).permitAll()
                // Role-gated endpoints
                .requestMatchers("/api/vet/**").hasAuthority("ROLE_VET")
                .requestMatchers("/api/pet-owner/**").hasAuthority("ROLE_PET_OWNER")
                // Everything else requires authentication
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt
                    .decoder(tokenVersionValidatingDecoder(jwtDecoder))
                    .jwtAuthenticationConverter(jwtAuthenticationConverter())
                )
            );

        return http.build();
    }

    @Bean
    @Order(3)
    public SecurityFilterChain fallbackPublicFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/", "/error", "/favicon.ico")
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }

    /**
     * Wraps the JwtDecoder to additionally validate that the "ver" claim matches
     * the user's current token_version — so we can revoke all tokens by bumping the version.
     */
    private JwtDecoder tokenVersionValidatingDecoder(JwtDecoder delegate) {
        return token -> {
            Jwt jwt = delegate.decode(token);
            String userId = jwt.getSubject();
            Integer tokenVer = jwt.getClaim("ver");

            if (userId != null && tokenVer != null) {
                userAuthRepo.findById(userId).ifPresent(user -> {
                    if (user.getTokenVersion() != tokenVer) {
                        throw new org.springframework.security.oauth2.jwt.JwtValidationException(
                                "Token version mismatch",
                                List.of(new org.springframework.security.oauth2.core.OAuth2Error("invalid_token", "Token version revoked", null))
                        );
                    }
                    if (!"ACTIVE".equals(user.getStatus())) {
                        throw new org.springframework.security.oauth2.jwt.JwtValidationException(
                                "Account is " + user.getStatus().toLowerCase(),
                                List.of(new org.springframework.security.oauth2.core.OAuth2Error("invalid_token", "Account blocked/deleted", null))
                        );
                    }
                });
            }

            return jwt;
        };
    }

    /**
     * Maps the "roles" claim in the JWT to Spring Security authorities.
     * No "SCOPE_" or "ROLE_" prefix added — we already include "ROLE_" in the claim value.
     */
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter grantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
        grantedAuthoritiesConverter.setAuthoritiesClaimName("roles");
        grantedAuthoritiesConverter.setAuthorityPrefix(""); // roles already have ROLE_ prefix

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(grantedAuthoritiesConverter);
        return converter;
    }

    private CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(appProperties.cors().allowedOrigin()));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }
}
