package com.pet.vetalya;

import com.pet.vetalya.config.AppProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(AppProperties.class)
public class VetalyaApplication {

	public static void main(String[] args) {
		SpringApplication.run(VetalyaApplication.class, args);
	}

}
