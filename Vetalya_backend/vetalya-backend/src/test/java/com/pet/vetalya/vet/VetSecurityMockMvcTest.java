package com.pet.vetalya.vet;

import com.pet.vetalya.config.ApiSecurityConfig;
import com.pet.vetalya.config.AppProperties;
import com.pet.vetalya.repository.UserAuthRepository;
import com.pet.vetalya.vet.controller.HospitalController;
import com.pet.vetalya.vet.dto.HospitalResponse;
import com.pet.vetalya.vet.service.HospitalService;
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

@WebMvcTest(HospitalController.class)
@Import({ApiSecurityConfig.class, VetSecurityMockMvcTest.TestConfig.class})
class VetSecurityMockMvcTest {

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
    private HospitalService hospitalService;

    @MockitoBean
    private UserAuthRepository userAuthRepo;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    @DisplayName("Scenario 2: Pet owner token gets 403 on /api/vet/**")
    void petOwnerTokenGets403OnVetEndpoints() throws Exception {
        mockMvc.perform(get("/api/vet/hospitals")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_PET_OWNER"))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Unauthenticated request gets 401 on /api/vet/**")
    void unauthenticatedRequestGets401() throws Exception {
        mockMvc.perform(get("/api/vet/hospitals"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Vet token gets 200 on /api/vet/**")
    void vetTokenGets200OnVetEndpoints() throws Exception {
        when(hospitalService.listOwn(anyString())).thenReturn(List.of(
                new HospitalResponse(
                        "hosp_1", "Pet Care", "Road 1", null, "Delhi", "DL", "110001",
                        "INDIA", "+919876543210", "care@pet.com", null, true, null, null,
                        0, Instant.now(), Instant.now()
                )
        ));

        mockMvc.perform(get("/api/vet/hospitals")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_VET"))
                        .jwt(j -> j.subject("vet_user_1"))))
                .andExpect(status().isOk());
    }
}
