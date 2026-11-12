package com.pet.vetalya.auth;

import com.pet.vetalya.config.AppProperties;
import com.pet.vetalya.common.GlobalExceptionHandler.BadRequestException;
import org.junit.jupiter.api.Test;
import org.springframework.web.cors.CorsConfiguration;
import static org.junit.jupiter.api.Assertions.*;
import java.util.List;

class ClientConfigurationTest {
    @Test void clientTypesAreExplicitAndDefaultToExistingWebBehavior() {
        assertTrue(ClientPlatform.parse(null).isWeb());
        assertEquals("android",ClientPlatform.parse("ANDROID").value());
        assertEquals(ClientPlatform.IOS,ClientPlatform.parse("ios"));
        assertThrows(BadRequestException.class,()->ClientPlatform.parse("admin"));
        assertThrows(BadRequestException.class,()->ClientPlatform.parse("desktop"));
    }
    @Test void corsAcceptsOnlyConfiguredWebOrigins() {
        var settings=new AppProperties.Cors("https://app.example.test, http://localhost:3000,https://app.example.test");
        assertEquals(List.of("https://app.example.test","http://localhost:3000"),settings.allowedOrigins());
        var cors=new CorsConfiguration();cors.setAllowedOrigins(settings.allowedOrigins());cors.setAllowCredentials(true);
        assertEquals("https://app.example.test",cors.checkOrigin("https://app.example.test"));
        assertNull(cors.checkOrigin("https://other.onrender.com"));
        assertNull(cors.checkOrigin("https://app.example.test.attacker.test"));
        assertNull(cors.checkOrigin("myvetapp://auth"));
    }
    @Test void invalidCorsConfigurationFailsClosed() {
        for(String origin:List.of("*","https://*.example.test","https://example.test/path","https://user:pass@example.test","https://example.test?token=x"))
            assertThrows(IllegalArgumentException.class,()->new AppProperties.Cors(origin).allowedOrigins());
        assertTrue(new AppProperties.Cors("").allowedOrigins().isEmpty());
    }
}
