package com.pet.vetalya.owner;

import com.pet.vetalya.config.ApiSecurityConfig;
import com.pet.vetalya.config.AppProperties;
import com.pet.vetalya.owner.controller.OwnerProfileController;
import com.pet.vetalya.owner.dto.OwnerProfileResponse;
import com.pet.vetalya.owner.service.OwnerProfileService;
import com.pet.vetalya.repository.UserAuthRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OwnerProfileController.class)
@Import({ApiSecurityConfig.class, OwnerSecurityMockMvcTest.TestConfig.class})
class OwnerSecurityMockMvcTest {

    @org.springframework.boot.test.context.TestConfiguration
    static class TestConfig {
        @org.springframework.context.annotation.Bean
        public AppProperties appProperties() {
            return new AppProperties(
                    "http://localhost:3000",
                    "myvetapp",
                    new AppProperties.Jwt("classpath:keys/private.pem", "classpath:keys/public.pem", 15, 14),
                    new AppProperties.Google(List.of("client-1")),
                    new AppProperties.Apple("team", "key", "service", "bundle", "classpath:keys/AuthKey.p8"),
                    new AppProperties.Cors("http://localhost:3000"),
                    new AppProperties.Appointment(30, 60, 2, List.of(24, 1)),
                    new AppProperties.Pet(20),
                    new AppProperties.Review(30),
                    "Asia/Kolkata"
            );
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OwnerProfileService ownerProfileService;

    @MockitoBean
    private UserAuthRepository userAuthRepo;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    @DisplayName("Vet token gets 403 on /api/owner/**")
    void vetTokenGets403OnOwnerEndpoints() throws Exception {
        mockMvc.perform(get("/api/owner/profile")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_VET"))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Unauthenticated request gets 401 on /api/owner/**")
    void unauthenticatedRequestGets401() throws Exception {
        mockMvc.perform(get("/api/owner/profile"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Pet owner token gets 200 on /api/owner/**")
    void petOwnerTokenGets200OnOwnerEndpoints() throws Exception {
        when(ownerProfileService.getProfile(anyString())).thenReturn(
                new OwnerProfileResponse(
                        "owner_1", "Jane", "Doe", "MG Road", null, "400001",
                        "Mumbai", "Maharashtra", "INDIA", true,
                        "jane@example.com", true, "+919876543210", true, "ACTIVE",
                        Instant.now(), Instant.now()
                )
        );

        mockMvc.perform(get("/api/owner/profile")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_PET_OWNER"))
                        .jwt(j -> j.subject("owner_1"))))
                .andExpect(status().isOk());
    }
}
