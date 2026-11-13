package com.pet.vetalya.staging;

import com.pet.vetalya.auth.controller.AuthController;
import com.pet.vetalya.auth.controller.SystemController;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class StagingExposureTest {
    @Test void publicDatabaseInspectionIsDisabledWithoutQueryingData() {
        var jdbc=mock(JdbcTemplate.class);
        assertEquals(404,new SystemController(jdbc).dbInspect().getStatusCode().value());
        verifyNoInteractions(jdbc);
    }
    @Test void frontendDeepLinksUseTheReactEntryPoint() throws Exception {
        var mvc=org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup(new com.pet.vetalya.auth.controller.StagingSpaController()).build();
        for(var path:java.util.List.of("/owner/login","/owner/register","/vet/login","/vet/register","/login"))
            mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(path))
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().contentTypeCompatibleWith("text/html"))
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().string(org.hamcrest.Matchers.containsString("id=\"root\"")));
    }
    @Test void anonymousDemoCannotObtainPlatformAdminSession() {
        var controller=mock(AuthController.class,CALLS_REAL_METHODS);
        assertEquals(404,controller.demoLogin(java.util.Map.of("role","PLATFORM_ADMIN"),null,null).getStatusCode().value());
    }
}
