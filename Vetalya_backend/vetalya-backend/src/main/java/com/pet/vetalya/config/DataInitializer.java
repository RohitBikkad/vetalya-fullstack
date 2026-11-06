package com.pet.vetalya.config;

import com.github.f4b6a3.ulid.UlidCreator;
import com.pet.vetalya.entity.AuthProvider;
import com.pet.vetalya.entity.PetOwner;
import com.pet.vetalya.entity.UserAuth;
import com.pet.vetalya.repository.AuthProviderRepository;
import com.pet.vetalya.repository.PetOwnerRepository;
import com.pet.vetalya.repository.UserAuthRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserAuthRepository userAuthRepo;
    private final AuthProviderRepository authProviderRepo;
    private final PetOwnerRepository petOwnerRepo;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;

    public DataInitializer(UserAuthRepository userAuthRepo,
                           AuthProviderRepository authProviderRepo,
                           PetOwnerRepository petOwnerRepo,
                           PasswordEncoder passwordEncoder,
                           JdbcTemplate jdbcTemplate) {
        this.userAuthRepo = userAuthRepo;
        this.authProviderRepo = authProviderRepo;
        this.petOwnerRepo = petOwnerRepo;
        this.passwordEncoder = passwordEncoder;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) {
        createOtpTable();
        upgradeRoleConstraint();
        seedAdminUser();
    }

    private void createOtpTable() {
        try {
            jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS otp_verification (
                    id BIGSERIAL PRIMARY KEY,
                    target VARCHAR(255) NOT NULL,
                    otp_code VARCHAR(10) NOT NULL,
                    purpose VARCHAR(30) NOT NULL,
                    attempts INTEGER NOT NULL DEFAULT 0,
                    verified BOOLEAN NOT NULL DEFAULT FALSE,
                    expires_at TIMESTAMPTZ NOT NULL,
                    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
                )
            """);
            jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_otp_target ON otp_verification(target)");
            log.info("✅ Database table otp_verification verified");
        } catch (Exception ex) {
            log.warn("Notice checking otp_verification table: {}", ex.getMessage());
        }
    }

    private void upgradeRoleConstraint() {
        try {
            jdbcTemplate.execute("ALTER TABLE user_auth ALTER COLUMN role TYPE VARCHAR(30)");
            jdbcTemplate.execute("ALTER TABLE user_auth DROP CONSTRAINT IF EXISTS user_auth_role_check");
            jdbcTemplate.execute("ALTER TABLE user_auth ADD CONSTRAINT user_auth_role_check CHECK (role IN ('ROLE_VET', 'ROLE_PET_OWNER', 'ROLE_PLATFORM_ADMIN', 'ROLE_CLINIC_ADMIN'))");
            log.info("✅ Database constraint user_auth_role_check upgraded to include ROLE_PLATFORM_ADMIN");
        } catch (Exception ex) {
            log.warn("Notice updating role constraint: {}", ex.getMessage());
        }
    }

    private void seedAdminUser() {
        String adminEmail = "admin@vetalya.com";
        try {
            var existing = userAuthRepo.findByEmail(adminEmail);
            if (existing.isEmpty()) {
                String userId = UlidCreator.getMonotonicUlid().toString();

                UserAuth admin = new UserAuth();
                admin.setUserId(userId);
                admin.setEmail(adminEmail);
                admin.setEmailVerified(true);
                admin.setPhone("+91 80 4719 0000");
                admin.setPhoneVerified(true);
                admin.setRole("ROLE_PLATFORM_ADMIN");
                admin.setStatus("ACTIVE");
                admin.setPasswordHash(passwordEncoder.encode("Admin@Vetalya2026!"));
                admin.setTokenVersion(0);
                userAuthRepo.save(admin);

                AuthProvider ap = new AuthProvider();
                ap.setUserId(userId);
                ap.setProvider("GOOGLE");
                ap.setProviderUserId("admin-vetalya-master");
                ap.setProviderEmail(adminEmail);
                authProviderRepo.save(ap);

                PetOwner profile = new PetOwner();
                profile.setUserId(userId);
                profile.setFirstName("Vetalya Platform");
                profile.setLastName("Administrator");
                petOwnerRepo.save(profile);

                log.info("✅ Pre-seeded Super Admin account created: {} (Role: ROLE_PLATFORM_ADMIN)", adminEmail);
            } else {
                UserAuth admin = existing.get();
                admin.setPasswordHash(passwordEncoder.encode("Admin@Vetalya2026!"));
                admin.setRole("ROLE_PLATFORM_ADMIN");
                admin.setStatus("ACTIVE");
                userAuthRepo.save(admin);
                log.info("✅ Verified Super Admin account credentials for: {}", adminEmail);
            }
        } catch (Exception e) {
            log.warn("Admin seeding notice: {}", e.getMessage());
        }
    }
}
