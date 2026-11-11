package com.pet.vetalya.config;

import com.pet.vetalya.logging.SecurityMdcFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Dedicated SecurityFilterChain for Actuator management endpoints.
 * Executed at @Order(0) before OAuth2 (@Order(1)) and Business API (@Order(2)) chains.
 * Liveness and readiness probes are open to container orchestrators without tokens.
 * All sensitive endpoints require HTTP Basic authentication with dedicated actuator admin credentials
 * and optional IP allow-listing.
 */
@Configuration
public class ActuatorSecurityConfig {

    @Value("${app.management.security.username:actuator-admin}")
    private String actuatorUsername;

    @Value("${app.management.security.password:ChangeMeInProd!Actuator9090}")
    private String actuatorPassword;

    @Value("${app.management.allowed-ips:127.0.0.1,0:0:0:0:0:0:0:1}")
    private String allowedIpsConfig;

    @Bean
    @Order(0)
    public SecurityFilterChain actuatorFilterChain(HttpSecurity http) throws Exception {
        Set<String> allowedIps = Arrays.stream(allowedIpsConfig.split(","))
                .map(s -> s.trim())
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());

        http
            .securityMatcher("/actuator/**")
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Unauthenticated access for liveness & readiness probes
                .requestMatchers(
                    "/actuator/health",
                    "/actuator/health/liveness",
                    "/actuator/health/readiness",
                    "/actuator/info"
                ).permitAll()
                // All other actuator endpoints require ACTUATOR_ADMIN role and allowed IP check
                .requestMatchers("/actuator/**").access((authentication, context) -> {
                    HttpServletRequest request = context.getRequest();
                    String remoteIp = request.getRemoteAddr();

                    boolean ipAllowed = allowedIps.contains("*") || allowedIps.contains(remoteIp);
                    boolean isAuthorized = authentication.get().getAuthorities().stream()
                            .anyMatch(a -> "ROLE_ACTUATOR_ADMIN".equals(a.getAuthority()));

                    return new AuthorizationDecision(ipAllowed && isAuthorized);
                })
                .anyRequest().authenticated()
            )
            .httpBasic(Customizer.withDefaults())
            .addFilterBefore(new SecurityMdcFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public UserDetailsService actuatorUserDetailsService(PasswordEncoder passwordEncoder) {
        UserDetails admin = User.builder()
                .username(actuatorUsername)
                .password(passwordEncoder.encode(actuatorPassword))
                .roles("ACTUATOR_ADMIN")
                .build();
        return new InMemoryUserDetailsManager(admin);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }
}

