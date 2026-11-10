package com.pet.vetalya;

import com.pet.vetalya.config.AppProperties;
import com.pet.vetalya.logging.LoggingProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.HashMap;
import java.util.Map;

@SpringBootApplication
@EnableConfigurationProperties({AppProperties.class, LoggingProperties.class})
public class VetalyaApplication {

	public static void main(String[] args) {
		SpringApplication app = new SpringApplication(VetalyaApplication.class);
		app.addListeners((ApplicationListener<ApplicationEnvironmentPreparedEvent>) event -> {
			ConfigurableEnvironment env = event.getEnvironment();
			String rawUrl = env.getProperty("spring.datasource.url");
			String rawUser = env.getProperty("spring.datasource.username");

			Map<String, Object> overrides = new HashMap<>();

			// 1. Sanitize or redirect obsolete/direct username to Supabase pooler tenant username
			if (rawUser == null || rawUser.isBlank() || rawUser.contains("evkdpnhbwajlmnpxeijb") || !rawUser.contains(".")) {
				overrides.put("spring.datasource.username", "postgres.lihsmcgfxjuldjenjdti");
			}

			// 2. Sanitize or redirect database URL to verified IPv4 Supabase pooler
			String targetUrl = rawUrl;
			if (targetUrl == null || targetUrl.isBlank() || targetUrl.contains("evkdpnhbwajlmnpxeijb") || targetUrl.contains("db.lihsmcgfxjuldjenjdti.supabase.co")) {
				targetUrl = "jdbc:postgresql://aws-0-ap-southeast-1.pooler.supabase.com:5432/postgres?sslmode=require&sslfactory=org.postgresql.ssl.NonValidatingFactory";
				overrides.put("spring.datasource.url", targetUrl);
			} else {
				if (!targetUrl.startsWith("jdbc:")) {
					targetUrl = "jdbc:" + targetUrl;
					overrides.put("spring.datasource.url", targetUrl);
				}
				if (!targetUrl.contains("sslfactory=")) {
					String separator = targetUrl.contains("?") ? "&" : "?";
					targetUrl = targetUrl + separator + "sslfactory=org.postgresql.ssl.NonValidatingFactory";
					overrides.put("spring.datasource.url", targetUrl);
				}
			}

			// 3. Fallback database password if missing
			String pass = env.getProperty("spring.datasource.password");
			if (pass == null || pass.isBlank()) {
				overrides.put("spring.datasource.password", "Vetalya@1409");
			}

			if (!overrides.isEmpty()) {
				env.getPropertySources().addFirst(new MapPropertySource("supabaseAutoHealingOverrides", overrides));
			}

			String activeUrl = env.getProperty("spring.datasource.url");
			String activeUser = env.getProperty("spring.datasource.username");
			System.out.println("================================================================================");
			System.out.println(" [VETALYA DB CONFIG] Active Supabase Target:");
			System.out.println("  -> URL:  " + activeUrl);
			System.out.println("  -> USER: " + activeUser);
			System.out.println("================================================================================");
		});
		app.run(args);
	}

}
