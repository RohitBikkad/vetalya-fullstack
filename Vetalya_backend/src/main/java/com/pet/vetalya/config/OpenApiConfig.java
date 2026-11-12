package com.pet.vetalya.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Value("${API_PUBLIC_URL:}")
    private String publicApiUrl;

    @Bean
    public OpenAPI customOpenAPI() {
        final String securitySchemeName = "bearerAuth";

        return new OpenAPI()
                .info(new Info()
                        .title("Vetalya Platform REST API")
                        .version("1.0.0")
                        .description("Production REST API documentation for Vetalya — Enterprise Digital Animal Healthcare & Veterinary Platform. " +
                                "Supports Pet Parents (digital passports, appointments, vaccination trackers), " +
                                "Veterinarians (live OPD queue, SOAP notes, digital Rx), and Platform Administrators.")
                        .contact(new Contact()
                                .name("Vetalya Engineering Team")
                                .email("contact@vetalya.com")
                                .url("https://vetalya-frontend.onrender.com"))
                        .license(new License()
                                .name("Proprietary")
                                .url("https://vetalya.com/terms")))
                .servers(publicApiUrl == null || publicApiUrl.isBlank()
                    ? List.of(new Server().url("/").description("Current API server — shared by web, Android and iOS"))
                    : List.of(new Server().url(publicApiUrl).description("Configured shared public API")))
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName,
                                new SecurityScheme()
                                        .name(securitySchemeName)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Enter your RS256 JWT access token obtained from /api/auth/login or /api/auth/register")));
    }
}

