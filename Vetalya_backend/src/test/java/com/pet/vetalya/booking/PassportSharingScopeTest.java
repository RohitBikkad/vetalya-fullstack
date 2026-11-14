package com.pet.vetalya.booking;

import com.pet.vetalya.clinic.ClinicalAccess;
import com.pet.vetalya.common.GlobalExceptionHandler.ForbiddenException;
import com.pet.vetalya.entity.Pet;
import com.pet.vetalya.entity.PetHealthPassport;
import com.pet.vetalya.entity.RecordShareRequest;
import com.pet.vetalya.entity.Vaccination;
import com.pet.vetalya.repository.*;
import com.pet.vetalya.service.PetPassportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PassportSharingScopeTest {
    private final PetRepository pets = mock(PetRepository.class);
    private final PetHealthPassportRepository passports = mock(PetHealthPassportRepository.class);
    private final RecordShareRequestRepository shares = mock(RecordShareRequestRepository.class);
    private final PetDocumentRepository documents = mock(PetDocumentRepository.class);
    private final AppointmentRepository appointments = mock(AppointmentRepository.class);
    private final VaccinationRepository vaccinations = mock(VaccinationRepository.class);
    private final ClinicalAccess access = mock(ClinicalAccess.class);
    private final PetPassportService service = new PetPassportService(pets, passports, null, shares, documents,
            vaccinations, appointments, null, access);
    private final RecordShareRequest grant = new RecordShareRequest();

    @BeforeEach void setup() {
        var pet = new Pet();
        pet.setPetId("pet");
        pet.setOwnerUserId("owner");
        when(pets.findById("pet")).thenReturn(Optional.of(pet));
        grant.setRequesterUserId("vet");
        grant.setTargetHospitalId("hospital");
        grant.setScope("VACCINES_ONLY");
        grant.setStatus("APPROVED");
        grant.setApprovedAt(Instant.now().minusSeconds(60));
        grant.setExpiresAt(Instant.now().plusSeconds(3600));
        when(shares.findAllByPetId("pet")).thenReturn(List.of(grant));
    }

    @Test void vaccinationScopeSkipsConsultationsAndDocuments() {
        var vaccination = mock(Vaccination.class);
        when(vaccination.getCreatedAt()).thenReturn(Instant.now());
        when(vaccinations.findAllByPetIdOrderByLastVaccinationDateDesc("pet")).thenReturn(List.of(vaccination));

        assertEquals("VACCINATION", service.getPetTimeline("vet", "pet", "hospital").getFirst().eventType());
        assertThrows(ForbiddenException.class, () -> service.getOrCreatePassport("vet", "pet", "hospital"));
        assertThrows(ForbiddenException.class, () -> service.listDocuments("vet", "pet", "hospital"));
        verifyNoInteractions(appointments, documents, passports);
    }

    @Test void wrongHospitalWrongRecipientAndRevokedMembershipAreDenied() {
        assertThrows(ForbiddenException.class, () -> service.getPetTimeline("vet", "pet", "other"));
        assertThrows(ForbiddenException.class, () -> service.getPetTimeline("vet", "pet", null));
        assertThrows(ForbiddenException.class, () -> service.getPetTimeline("another-vet", "pet", "hospital"));
        doThrow(new AccessDeniedException("Inactive membership")).when(access).requireVeterinarian("vet", "hospital");
        assertThrows(AccessDeniedException.class, () -> service.getPetTimeline("vet", "pet", "hospital"));
        verifyNoInteractions(vaccinations, appointments, documents);
    }

    @Test void expiredAndRevokedGrantsAreDeniedAndOwnerAccessStillWorks() {
        grant.setExpiresAt(Instant.now().minusSeconds(1));
        assertThrows(ForbiddenException.class, () -> service.getPetTimeline("vet", "pet", "hospital"));
        grant.setExpiresAt(Instant.now().plusSeconds(3600));
        grant.setStatus("REVOKED");
        assertThrows(ForbiddenException.class, () -> service.getPetTimeline("vet", "pet", "hospital"));
        assertTrue(service.getPetTimeline("owner", "pet", null).isEmpty());
        verify(appointments).findAllByPetIdAndStatus("pet", "COMPLETED");
    }

    @Test void emergencySummaryOmitsOwnerContactAndCannotReadTimeline() {
        grant.setScope("EMERGENCY_SUMMARY");
        var passport = new PetHealthPassport();
        passport.setAllergiesSummary("Example allergy");
        passport.setEmergencyContactPhone("private");
        passport.setPublicEmergencyQrHash("private-token");
        passport.setDietaryNotes("private notes");
        when(passports.findByPetId("pet")).thenReturn(Optional.of(passport));

        var summary = service.getOrCreatePassport("vet", "pet", "hospital");
        assertEquals("Example allergy", summary.allergiesSummary());
        assertNull(summary.emergencyContactPhone());
        assertNull(summary.publicEmergencyQrHash());
        assertNull(summary.dietaryNotes());
        assertThrows(ForbiddenException.class, () -> service.getPetTimeline("vet", "pet", "hospital"));
    }
}
