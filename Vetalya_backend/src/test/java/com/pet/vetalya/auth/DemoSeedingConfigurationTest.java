package com.pet.vetalya.auth;

import com.pet.vetalya.config.DataInitializer;
import com.pet.vetalya.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class DemoSeedingConfigurationTest {
    private final ApplicationContextRunner context = new ApplicationContextRunner()
            .withUserConfiguration(DataInitializer.class);

    @Test void normalStartupDoesNotCreateDemoInitializer() {
        context.run(c -> assertThat(c).doesNotHaveBean(DataInitializer.class));
    }
    @Test void explicitDisabledSettingDoesNotCreateDemoInitializer() {
        context.withPropertyValues("app.seed-demo-data=false")
                .run(c -> assertThat(c).doesNotHaveBean(DataInitializer.class));
    }
    @Test void isolatedDemoCanExplicitlyEnableInitializer() {
        context.withPropertyValues("app.seed-demo-data=true")
                .withBean(UserAuthRepository.class, () -> mock(UserAuthRepository.class))
                .withBean(AuthProviderRepository.class, () -> mock(AuthProviderRepository.class))
                .withBean(PetOwnerRepository.class, () -> mock(PetOwnerRepository.class))
                .withBean(VetRepository.class, () -> mock(VetRepository.class))
                .withBean(HospitalRepository.class, () -> mock(HospitalRepository.class))
                .withBean(PetRepository.class, () -> mock(PetRepository.class))
                .withBean(PetHealthPassportRepository.class, () -> mock(PetHealthPassportRepository.class))
                .withBean(PasswordEncoder.class, () -> mock(PasswordEncoder.class))
                .withBean(JdbcTemplate.class, () -> mock(JdbcTemplate.class))
                .run(c -> assertThat(c).hasSingleBean(DataInitializer.class));
    }
}
