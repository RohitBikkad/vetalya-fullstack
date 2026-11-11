package com.pet.vetalya.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.stream.Collectors;

/**
 * Populates authenticated userId (from JWT "sub") and granted roles into MDC.
 * Strictly guarantees that personal identifiers (email, phone, name) and raw
 * tokens are never added.
 */
public class SecurityMdcFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean populated = false;

        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            String userId = null;
            if (auth.getPrincipal() instanceof Jwt jwt) {
                userId = jwt.getSubject();
            } else if (auth.getName() != null && !"anonymousUser".equals(auth.getName())) {
                userId = auth.getName();
            }

            if (userId != null && !userId.isBlank()) {
                MDC.put("userId", userId);
                populated = true;
            }

            String roles = auth.getAuthorities().stream()
                    .map(a -> a.getAuthority())
                    .collect(Collectors.joining(","));
            if (!roles.isBlank()) {
                MDC.put("role", roles);
                populated = true;
            }
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            if (populated) {
                MDC.remove("userId");
                MDC.remove("role");
            }
        }
    }
}
