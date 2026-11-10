package com.pet.vetalya.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.util.pattern.PathPatternParser;

/**
 * Global Spring MVC configuration.
 *
 * <p>Spring Boot 3+ removed trailing-slash redirect by default.
 * Meta's WhatsApp webhook verifier sends requests to the exact URL you configure,
 * including any trailing slash. This configures explicit path patterns so both
 * {@code /webhook} and {@code /webhook/} are accepted without redirects.
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        // Allow trailing slashes on all endpoints (removed in Spring Boot 3+ by default).
        // This prevents 404/500 when Meta or other callers append a trailing slash.
        PathPatternParser parser = new PathPatternParser();
        parser.setMatchOptionalTrailingSeparator(true);
        configurer.setPatternParser(parser);
    }
}
