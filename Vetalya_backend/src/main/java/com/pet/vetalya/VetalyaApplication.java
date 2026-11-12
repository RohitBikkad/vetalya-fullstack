package com.pet.vetalya;

import com.pet.vetalya.config.AppProperties;
import com.pet.vetalya.logging.LoggingProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({AppProperties.class, LoggingProperties.class})
public class VetalyaApplication {
    public static void main(String[] args) {
        // Spring's normal precedence respects environment variables, profiles and command-line overrides.
        SpringApplication.run(VetalyaApplication.class, args);
    }
}
