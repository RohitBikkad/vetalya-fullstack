package com.pet.vetalya.clinical;

import com.pet.vetalya.clinic.ClinicalAccess;
import com.pet.vetalya.common.GlobalExceptionHandler.ForbiddenException;
import com.pet.vetalya.common.IdGenerator;
import com.pet.vetalya.dto.DiagnosticItemRequest;
import com.pet.vetalya.dto.DiagnosticOrderRequest;
import com.pet.vetalya.entity.Pet;
import com.pet.vetalya.repository.*;
import com.pet.vetalya.service.DiagnosticService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class DiagnosticOrderScopeTest {
    @Test
    void rejectsAnEncounterOutsideTheOrdersPatientHospitalAndVetScope() {
        var orders = mock(DiagnosticOrderRepository.class);
        var pets = mock(PetRepository.class);
        var appointments = mock(AppointmentRepository.class);
        var encounters = mock(ClinicalEncounterRepository.class);
        var access = mock(ClinicalAccess.class);
        var service = new DiagnosticService(orders, mock(DiagnosticOrderItemRepository.class),
                mock(DiagnosticTestCatalogRepository.class), pets, new IdGenerator(), appointments, encounters, access);
        when(pets.findById("patient")).thenReturn(Optional.of(new Pet()));
        when(appointments.existsByVetUserIdAndPetIdAndHospitalId("vet", "patient", "hospital")).thenReturn(true);
        var request = new DiagnosticOrderRequest("patient", "hospital", "other-encounter", "ROUTINE", null,
                List.of(new DiagnosticItemRequest("CBC_CANINE", "Complete Blood Count", "HEMATOLOGY")));

        assertThrows(ForbiddenException.class, () -> service.createOrder("vet", request));
        verify(access).requireVeterinarian("vet", "hospital");
        verify(encounters).existsByEncounterIdAndPetIdAndHospitalIdAndVetUserId(
                "other-encounter", "patient", "hospital", "vet");
        verifyNoInteractions(orders);
    }
}
