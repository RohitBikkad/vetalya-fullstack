package com.pet.vetalya.audit.event;

import com.pet.vetalya.audit.service.AuditService;
import com.pet.vetalya.metrics.AppMetrics;
import com.pet.vetalya.metrics.BruteForceSignalService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.authorization.event.AuthorizationDeniedEvent;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * Listens to Spring Security framework authentication and authorization events,
 * forwarding them to the AuditService and recording operational metrics.
 */
@Component
public class SecurityAuditEventListener {

    private final AuditService auditService;
    private final AppMetrics appMetrics;
    private final BruteForceSignalService bruteForceSignalService;

    public SecurityAuditEventListener(
            AuditService auditService,
            AppMetrics appMetrics,
            BruteForceSignalService bruteForceSignalService) {
        this.auditService = auditService;
        this.appMetrics = appMetrics;
        this.bruteForceSignalService = bruteForceSignalService;
    }

    @EventListener
    public void onAuthenticationSuccess(AuthenticationSuccessEvent event) {
        Authentication auth = event.getAuthentication();
        String userId = null;
        if (auth.getPrincipal() instanceof Jwt jwt) {
            userId = jwt.getSubject();
        } else {
            userId = auth.getName();
        }

        String role = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));

        HttpServletRequest request = getCurrentRequest();
        String ip = auditService.resolveClientIp(request);
        String clientType = auditService.resolveClientType(request);
        String deviceId = auditService.resolveDeviceId(request);
        String userAgent = auditService.resolveUserAgent(request);

        appMetrics.recordLogin("jwt", "success");
        if (userId != null) {
            appMetrics.markUserActive(userId);
        }

        auditService.recordEvent(
                "LOGIN_SUCCESS",
                userId,
                role,
                "JWT",
                "SUCCESS",
                null,
                clientType,
                deviceId,
                ip,
                userAgent,
                Map.of("principal", userId != null ? userId : "unknown")
        );
    }

    @EventListener
    public void onAuthenticationFailure(AbstractAuthenticationFailureEvent event) {
        Authentication auth = event.getAuthentication();
        String principal = auth != null ? auth.getName() : "unknown";

        HttpServletRequest request = getCurrentRequest();
        String ip = auditService.resolveClientIp(request);
        String clientType = auditService.resolveClientType(request);
        String deviceId = auditService.resolveDeviceId(request);
        String userAgent = auditService.resolveUserAgent(request);

        appMetrics.recordLogin("jwt", "failure");
        if (ip != null) {
            bruteForceSignalService.recordIpFailure(ip);
        }

        auditService.recordEvent(
                "LOGIN_FAILURE",
                null,
                null,
                "JWT",
                "FAILURE",
                event.getException().getMessage(),
                clientType,
                deviceId,
                ip,
                userAgent,
                Map.of("attempted_principal", principal)
        );
    }

    @EventListener
    public void onAuthorizationDenied(AuthorizationDeniedEvent<?> event) {
        Authentication auth = event.getAuthentication().get();
        String userId = null;
        String role = null;
        if (auth != null && auth.isAuthenticated()) {
            if (auth.getPrincipal() instanceof Jwt jwt) {
                userId = jwt.getSubject();
            } else {
                userId = auth.getName();
            }
            role = auth.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.joining(","));
        }

        HttpServletRequest request = getCurrentRequest();
        String ip = auditService.resolveClientIp(request);
        String clientType = auditService.resolveClientType(request);
        String deviceId = auditService.resolveDeviceId(request);
        String userAgent = auditService.resolveUserAgent(request);

        auditService.recordEvent(
                "ACCESS_DENIED",
                userId,
                role,
                null,
                "FAILURE",
                "Access Denied (403)",
                clientType,
                deviceId,
                ip,
                userAgent,
                Map.of("resource", request != null ? request.getRequestURI() : "unknown")
        );
    }

    private HttpServletRequest getCurrentRequest() {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes != null ? attributes.getRequest() : null;
    }
}

