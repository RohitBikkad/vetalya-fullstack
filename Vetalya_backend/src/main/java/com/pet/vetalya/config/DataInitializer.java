package com.pet.vetalya.config;

import com.github.f4b6a3.ulid.UlidCreator;
import com.pet.vetalya.entity.*;
import com.pet.vetalya.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Component
@ConditionalOnProperty(name = "app.seed-demo-data", havingValue = "true")
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);
    private static final String DEFAULT_PASSWORD = "Password@123";

    private final UserAuthRepository userAuthRepo;
    private final AuthProviderRepository authProviderRepo;
    private final PetOwnerRepository petOwnerRepo;
    private final VetRepository vetRepo;
    private final HospitalRepository hospitalRepo;
    private final PetRepository petRepo;
    private final PetHealthPassportRepository passportRepo;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;

    public DataInitializer(UserAuthRepository userAuthRepo,
                           AuthProviderRepository authProviderRepo,
                           PetOwnerRepository petOwnerRepo,
                           VetRepository vetRepo,
                           HospitalRepository hospitalRepo,
                           PetRepository petRepo,
                           PetHealthPassportRepository passportRepo,
                           PasswordEncoder passwordEncoder,
                           JdbcTemplate jdbcTemplate) {
        this.userAuthRepo = userAuthRepo;
        this.authProviderRepo = authProviderRepo;
        this.petOwnerRepo = petOwnerRepo;
        this.vetRepo = vetRepo;
        this.hospitalRepo = hospitalRepo;
        this.petRepo = petRepo;
        this.passportRepo = passportRepo;
        this.passwordEncoder = passwordEncoder;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) {
        createOtpTable();
        upgradeRoleConstraint();
        seedAdminUser();
        String hospitalId = seedVetsAndHospital();
        seedPetParentAndPets();
        seedClinicAdmin(hospitalId);
        seedStaffUsers(hospitalId);
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
            jdbcTemplate.execute("""
                ALTER TABLE user_auth ADD CONSTRAINT user_auth_role_check CHECK (role IN (
                    'ROLE_VET', 'ROLE_PET_OWNER', 'ROLE_PLATFORM_ADMIN', 'ROLE_CLINIC_ADMIN',
                    'ROLE_LAB_TECHNICIAN', 'ROLE_RECEPTIONIST', 'ROLE_PHARMACIST', 'ROLE_VET_NURSE',
                    'ROLE_ACCOUNTANT', 'ROLE_SERVICE_PROVIDER', 'ROLE_SUPPORT', 'ROLE_CLINIC_STAFF'
                ))
            """);
            log.info("✅ Database constraint user_auth_role_check upgraded to include all platform roles");
        } catch (Exception ex) {
            log.warn("Notice updating role constraint: {}", ex.getMessage());
        }
    }

    private void seedAdminUser() {
        String adminEmail = "admin@vetalya.com";
        try {
            var existing = userAuthRepo.findByEmail(adminEmail);
            String userId = existing.map(UserAuth::getUserId).orElseGet(() -> UlidCreator.getMonotonicUlid().toString());

            UserAuth admin = existing.orElseGet(UserAuth::new);
            admin.setUserId(userId);
            admin.setEmail(adminEmail);
            admin.setEmailVerified(true);
            admin.setPhone("+91 80 4719 0000");
            admin.setPhoneVerified(true);
            admin.setRole("ROLE_PLATFORM_ADMIN");
            admin.setStatus("ACTIVE");
            admin.setPasswordHash(passwordEncoder.encode(DEFAULT_PASSWORD));
            admin.setTokenVersion(0);
            userAuthRepo.save(admin);

            if (existing.isEmpty()) {
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
            }
            log.info("✅ Super Admin ready: {} (Password: {})", adminEmail, DEFAULT_PASSWORD);
        } catch (Exception e) {
            log.warn("Admin seeding notice: {}", e.getMessage());
        }
    }

    private String seedVetsAndHospital() {
        String hospitalId = null;
        try {
            // Vet 1: Dr. Priya Sharma
            String priyaEmail = "dr.priya@vetalya.com";
            var existingPriya = userAuthRepo.findByEmail(priyaEmail);
            String priyaId = existingPriya.map(UserAuth::getUserId).orElseGet(() -> UlidCreator.getMonotonicUlid().toString());

            UserAuth priyaUser = existingPriya.orElseGet(UserAuth::new);
            priyaUser.setUserId(priyaId);
            priyaUser.setEmail(priyaEmail);
            priyaUser.setEmailVerified(true);
            priyaUser.setPhone("+91 98450 12345");
            priyaUser.setPhoneVerified(true);
            priyaUser.setRole("ROLE_VET");
            priyaUser.setStatus("ACTIVE");
            priyaUser.setPasswordHash(passwordEncoder.encode(DEFAULT_PASSWORD));
            userAuthRepo.save(priyaUser);

            if (vetRepo.findById(priyaId).isEmpty()) {
                Vet vet = new Vet();
                vet.setUserId(priyaId);
                vet.setFirstName("Priya");
                vet.setLastName("Sharma");
                vet.setRegistrationNo("KVC-2022-8192");
                vet.setVerified(true);
                vet.setPhotoUrl("https://images.unsplash.com/photo-1559839734-2b71ea197ec2?auto=format&fit=crop&w=200&q=80");
                vetRepo.save(vet);
            }

            // Vet 2: Dr. Arjun Patel (Surgery & Critical Care)
            String arjunEmail = "dr.arjun@vetalya.com";
            var existingArjun = userAuthRepo.findByEmail(arjunEmail);
            String arjunId = existingArjun.map(UserAuth::getUserId).orElseGet(() -> UlidCreator.getMonotonicUlid().toString());

            UserAuth arjunUser = existingArjun.orElseGet(UserAuth::new);
            arjunUser.setUserId(arjunId);
            arjunUser.setEmail(arjunEmail);
            arjunUser.setEmailVerified(true);
            arjunUser.setPhone("+91 98450 67890");
            arjunUser.setPhoneVerified(true);
            arjunUser.setRole("ROLE_VET");
            arjunUser.setStatus("ACTIVE");
            arjunUser.setPasswordHash(passwordEncoder.encode(DEFAULT_PASSWORD));
            userAuthRepo.save(arjunUser);

            if (vetRepo.findById(arjunId).isEmpty()) {
                Vet vet = new Vet();
                vet.setUserId(arjunId);
                vet.setFirstName("Arjun");
                vet.setLastName("Patel");
                vet.setRegistrationNo("MVC-2020-5412");
                vet.setVerified(true);
                vet.setPhotoUrl("https://images.unsplash.com/photo-1622253692010-333f2da6031d?auto=format&fit=crop&w=200&q=80");
                vetRepo.save(vet);
            }

            // Seed hospital
            var hospitals = hospitalRepo.findAllByVetUserId(priyaId);
            if (hospitals.isEmpty()) {
                hospitalId = UlidCreator.getMonotonicUlid().toString();
                Hospital h = new Hospital();
                h.setHospitalId(hospitalId);
                h.setVetUserId(priyaId);
                h.setName("Koramangala Companion Hospital");
                h.setAddress1("80 Feet Road, 4th Block, Koramangala");
                h.setCity("Bengaluru");
                h.setState("Karnataka");
                h.setPincode("560034");
                h.setCountry("INDIA");
                h.setMobile("+91 80 4719 2200");
                h.setEmail("contact@koramangalavet.in");
                h.setIs24x7(true);
                h.setActive(true);
                hospitalRepo.save(h);
            } else {
                hospitalId = hospitals.get(0).getHospitalId();
            }

            log.info("✅ Veterinarians ready: {} & {} (Password: {})", priyaEmail, arjunEmail, DEFAULT_PASSWORD);
        } catch (Exception e) {
            log.warn("Vet seeding notice: {}", e.getMessage());
        }
        return hospitalId;
    }

    private void seedPetParentAndPets() {
        String parentEmail = "parent@vetalya.com";
        try {
            var existing = userAuthRepo.findByEmail(parentEmail);
            String userId = existing.map(UserAuth::getUserId).orElseGet(() -> UlidCreator.getMonotonicUlid().toString());

            UserAuth parent = existing.orElseGet(UserAuth::new);
            parent.setUserId(userId);
            parent.setEmail(parentEmail);
            parent.setEmailVerified(true);
            parent.setPhone("+91 98765 43210");
            parent.setPhoneVerified(true);
            parent.setRole("ROLE_PET_OWNER");
            parent.setStatus("ACTIVE");
            parent.setPasswordHash(passwordEncoder.encode(DEFAULT_PASSWORD));
            userAuthRepo.save(parent);

            if (petOwnerRepo.findById(userId).isEmpty()) {
                PetOwner profile = new PetOwner();
                profile.setUserId(userId);
                profile.setFirstName("Rahul");
                profile.setLastName("Verma");
                petOwnerRepo.save(profile);
            }

            // Seed Pet 1: Bella (Canine - Golden Retriever)
            String bellaId = "01ARZ3NDEKTSV4RRFFQ69GPET1";
            if (petRepo.findById(bellaId).isEmpty()) {
                Pet bella = new Pet();
                bella.setPetId(bellaId);
                bella.setOwnerUserId(userId);
                bella.setName("Bella");
                bella.setSpecies("Canine");
                bella.setBreed("Golden Retriever");
                bella.setGender("FEMALE");
                bella.setDob(LocalDate.now().minusYears(3));
                bella.setColor("Golden Fawn");
                bella.setMicrochipNumber("982000192837461");
                bella.setPassportNumber("VP-2026-KA-001092");
                bella.setWeightKg(new BigDecimal("28.50"));
                bella.setSterilizationStatus("STERILIZED");
                bella.setPhotoUrl("https://images.unsplash.com/photo-1552053831-71594a27632d?auto=format&fit=crop&w=400&q=80");
                petRepo.save(bella);

                PetHealthPassport p1 = new PetHealthPassport();
                p1.setPassportId(UlidCreator.getMonotonicUlid().toString());
                p1.setPetId(bellaId);
                p1.setPassportNumber("VP-2026-KA-001092");
                p1.setBloodGroup("DEA 1.1 Negative");
                p1.setAllergiesSummary("Mild allergy to poultry by-products");
                p1.setChronicConditionsSummary("None; regular preventive care");
                p1.setDietaryNotes("Royal Canin Maxi Adult with salmon oil supplement");
                p1.setEmergencyContactPhone("+91 98765 43210");
                p1.setPublicEmergencyQrHash("qr-bella-emergency-hash-2026");
                passportRepo.save(p1);
            }

            // Seed Pet 2: Leo (Feline - Persian)
            String leoId = "01ARZ3NDEKTSV4RRFFQ69GPET2";
            if (petRepo.findById(leoId).isEmpty()) {
                Pet leo = new Pet();
                leo.setPetId(leoId);
                leo.setOwnerUserId(userId);
                leo.setName("Leo");
                leo.setSpecies("Feline");
                leo.setBreed("Persian Longhair");
                leo.setGender("MALE");
                leo.setDob(LocalDate.now().minusMonths(18));
                leo.setColor("Pure White");
                leo.setMicrochipNumber("982000582910384");
                leo.setPassportNumber("VP-2026-KA-001093");
                leo.setWeightKg(new BigDecimal("4.20"));
                leo.setSterilizationStatus("STERILIZED");
                leo.setPhotoUrl("https://images.unsplash.com/photo-1514888286974-6c03e2ca1dba?auto=format&fit=crop&w=400&q=80");
                petRepo.save(leo);

                PetHealthPassport p2 = new PetHealthPassport();
                p2.setPassportId(UlidCreator.getMonotonicUlid().toString());
                p2.setPetId(leoId);
                p2.setPassportNumber("VP-2026-KA-001093");
                p2.setBloodGroup("Type A");
                p2.setAllergiesSummary("None known");
                p2.setChronicConditionsSummary("History of hairball formation; brush weekly");
                p2.setDietaryNotes("Indoor adult wet & dry combo");
                p2.setEmergencyContactPhone("+91 98765 43210");
                p2.setPublicEmergencyQrHash("qr-leo-emergency-hash-2026");
                passportRepo.save(p2);
            }

            log.info("✅ Pet Parent ready: {} (Password: {}) with pets Bella & Leo", parentEmail, DEFAULT_PASSWORD);
        } catch (Exception e) {
            log.warn("Pet parent seeding notice: {}", e.getMessage());
        }
    }

    private void seedClinicAdmin(String hospitalId) {
        String adminEmail = "clinic.admin@vetalya.com";
        try {
            var existing = userAuthRepo.findByEmail(adminEmail);
            String userId = existing.map(UserAuth::getUserId).orElseGet(() -> UlidCreator.getMonotonicUlid().toString());

            UserAuth admin = existing.orElseGet(UserAuth::new);
            admin.setUserId(userId);
            admin.setEmail(adminEmail);
            admin.setEmailVerified(true);
            admin.setPhone("+91 98111 22334");
            admin.setPhoneVerified(true);
            admin.setRole("ROLE_CLINIC_ADMIN");
            admin.setStatus("ACTIVE");
            admin.setPasswordHash(passwordEncoder.encode(DEFAULT_PASSWORD));
            userAuthRepo.save(admin);

            if (petOwnerRepo.findById(userId).isEmpty()) {
                PetOwner profile = new PetOwner();
                profile.setUserId(userId);
                profile.setFirstName("Sunita");
                profile.setLastName("Deshmukh");
                petOwnerRepo.save(profile);
            }
            log.info("✅ Clinic Administrator ready: {} (Password: {})", adminEmail, DEFAULT_PASSWORD);
        } catch (Exception e) {
            log.warn("Clinic admin seeding notice: {}", e.getMessage());
        }
    }

    private void seedStaffUsers(String hospitalId) {
        // Receptionist
        createStaffUser("reception@vetalya.com", "Ananya", "Rao", "ROLE_RECEPTIONIST", "+91 98222 33445");
        // Laboratory Technician
        createStaffUser("lab.tech@vetalya.com", "Vikram", "Nair", "ROLE_LAB_TECHNICIAN", "+91 98333 44556");
        // Pharmacist
        createStaffUser("pharmacist@vetalya.com", "Rajesh", "Kulkarni", "ROLE_PHARMACIST", "+91 98444 55667");
        // Veterinary Nurse
        createStaffUser("nurse@vetalya.com", "Deepa", "Menon", "ROLE_VET_NURSE", "+91 98555 66778");
    }

    private void createStaffUser(String email, String firstName, String lastName, String role, String phone) {
        try {
            var existing = userAuthRepo.findByEmail(email);
            String userId = existing.map(UserAuth::getUserId).orElseGet(() -> UlidCreator.getMonotonicUlid().toString());

            UserAuth user = existing.orElseGet(UserAuth::new);
            user.setUserId(userId);
            user.setEmail(email);
            user.setEmailVerified(true);
            user.setPhone(phone);
            user.setPhoneVerified(true);
            user.setRole(role);
            user.setStatus("ACTIVE");
            user.setPasswordHash(passwordEncoder.encode(DEFAULT_PASSWORD));
            userAuthRepo.save(user);

            if (petOwnerRepo.findById(userId).isEmpty()) {
                PetOwner profile = new PetOwner();
                profile.setUserId(userId);
                profile.setFirstName(firstName);
                profile.setLastName(lastName);
                petOwnerRepo.save(profile);
            }
            log.info("✅ Staff ready: {} ({}) - Password: {}", email, role, DEFAULT_PASSWORD);
        } catch (Exception e) {
            log.warn("Staff user seeding notice for {}: {}", email, e.getMessage());
        }
    }
}
