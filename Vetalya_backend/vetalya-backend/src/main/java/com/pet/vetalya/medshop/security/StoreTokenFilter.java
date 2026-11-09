package com.pet.vetalya.medshop.security;

import com.pet.vetalya.medshop.common.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filter for /api/store/** requests that enforces token version, active user,
 * active tenant, and sets TenantContext and MDC safely.
 */
public class StoreTokenFilter extends OncePerRequestFilter {

    private final StoreUserStatusCache userStatusCache;

    public StoreTokenFilter(StoreUserStatusCache userStatusCache) {
        this.userStatusCache = userStatusCache;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return !uri.startsWith("/api/store/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Jwt jwt) {
            String tenantId = jwt.getClaimAsString("tenant_id");
            String userId = jwt.getSubject();
            Long verLong = jwt.getClaim("ver");
            Integer ver = verLong != null ? verLong.intValue() : null;

            if (tenantId == null || userId == null || !userStatusCache.isTokenValid(tenantId, userId, ver)) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/problem+json");
                response.getWriter().write("""
                    {"status":401,"title":"Unauthorized","detail":"Token is expired, revoked, or user/tenant is inactive"}
                """);
                return;
            }

            TenantContext.setTenantId(tenantId);
            TenantContext.setUserId(userId);
            MDC.put("tenantId", tenantId);
            MDC.put("userId", userId);
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
            MDC.remove("tenantId");
            MDC.remove("userId");
        }
    }
}

