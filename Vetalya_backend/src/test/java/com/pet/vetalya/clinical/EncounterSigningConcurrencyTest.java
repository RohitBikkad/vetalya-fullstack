package com.pet.vetalya.clinical;

import com.pet.vetalya.entity.Appointment;
import com.pet.vetalya.entity.ClinicalEncounter;
import com.pet.vetalya.entity.Hospital;
import com.pet.vetalya.entity.Pet;
import com.pet.vetalya.entity.PetOwner;
import com.pet.vetalya.entity.Vet;
import jakarta.persistence.LockModeType;
import org.hibernate.cfg.Configuration;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class EncounterSigningConcurrencyTest {
    @Test
    void waitingDraftWriterSeesTheCommittedSignatureBeforeEditing() throws Exception {
        var config = new Configuration().addAnnotatedClass(ClinicalEncounter.class)
                .addAnnotatedClass(Appointment.class).addAnnotatedClass(Pet.class)
                .addAnnotatedClass(PetOwner.class).addAnnotatedClass(Hospital.class).addAnnotatedClass(Vet.class)
                .setProperty("hibernate.connection.url", "jdbc:h2:mem:" + UUID.randomUUID())
                .setProperty("hibernate.connection.username", "sa")
                .setProperty("hibernate.hbm2ddl.auto", "create-drop");
        try (var factory = config.buildSessionFactory();
             var executor = Executors.newSingleThreadExecutor();
             var signer = factory.openSession()) {
            var encounter = new ClinicalEncounter();
            encounter.setEncounterId("encounter");
            encounter.setVetUserId("vet");
            encounter.setPetId("pet");
            var pet = new Pet();
            pet.setPetId("pet");
            pet.setOwnerUserId("owner");
            pet.setName("Patient");
            pet.setSpecies("DOG");
            factory.inTransaction(session -> {
                session.persist(pet);
                session.persist(encounter);
            });

            var signing = signer.beginTransaction();
            signer.find(ClinicalEncounter.class, "encounter", LockModeType.PESSIMISTIC_WRITE).setSigned(true);

            var draftStarted = new CountDownLatch(1);
            var draft = executor.submit(() -> factory.fromTransaction(session -> {
                draftStarted.countDown();
                return session.find(ClinicalEncounter.class, "encounter", LockModeType.PESSIMISTIC_WRITE).isSigned();
            }));
            try {
                assertTrue(draftStarted.await(5, TimeUnit.SECONDS));
                assertThrows(java.util.concurrent.TimeoutException.class, () -> draft.get(200, TimeUnit.MILLISECONDS));
            } finally {
                signing.commit();
            }
            assertTrue(draft.get(5, TimeUnit.SECONDS));
        }
    }
}
