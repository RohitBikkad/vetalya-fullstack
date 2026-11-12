package com.pet.vetalya.booking;

import com.pet.vetalya.common.IdGenerator;
import com.pet.vetalya.common.GlobalExceptionHandler.ConflictException;
import com.pet.vetalya.dto.BookSlotRequest;
import com.pet.vetalya.entity.*;
import com.pet.vetalya.repository.*;
import com.pet.vetalya.service.AppointmentSlotService;
import org.junit.jupiter.api.Test;
import java.time.*;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class OwnerSlotBookingTest {
    @Test void alreadyStartedSlotsAreRejectedWithoutWrites() {
        var slots=mock(AppointmentSlotRepository.class);var appointments=mock(AppointmentRepository.class);var pets=mock(PetRepository.class);
        var slot=slot(LocalDate.now(ZoneId.of("Asia/Kolkata")).minusDays(1));
        when(slots.findByIdWithPessimisticLock("slot")).thenReturn(Optional.of(slot));
        var service=new AppointmentSlotService(slots,appointments,pets,new IdGenerator());
        assertThrows(ConflictException.class,()->service.bookSlotAtomically("owner",new BookSlotRequest("slot","pet","Checkup","OPD_CONSULTATION")));
        verify(slots,never()).save(any());verifyNoInteractions(pets,appointments);
    }
    @Test void anotherOwnersPetCannotBeBooked() {
        var slots=mock(AppointmentSlotRepository.class);var appointments=mock(AppointmentRepository.class);var pets=mock(PetRepository.class);
        when(slots.findByIdWithPessimisticLock("slot")).thenReturn(Optional.of(slot(LocalDate.now().plusDays(2))));
        var pet=new Pet();pet.setPetId("pet");pet.setOwnerUserId("another-owner");when(pets.findById("pet")).thenReturn(Optional.of(pet));
        var service=new AppointmentSlotService(slots,appointments,pets,new IdGenerator());
        assertThrows(ConflictException.class,()->service.bookSlotAtomically("owner",new BookSlotRequest("slot","pet","Checkup",null)));
        verify(slots,never()).save(any());verifyNoInteractions(appointments);
    }
    @Test void successfulBookingConsumesCapacityAndRetainsRealIdentity() {
        var slots=mock(AppointmentSlotRepository.class);var appointments=mock(AppointmentRepository.class);var pets=mock(PetRepository.class);
        var slot=slot(LocalDate.now().plusDays(2));when(slots.findByIdWithPessimisticLock("slot")).thenReturn(Optional.of(slot));
        var pet=new Pet();pet.setPetId("pet");pet.setOwnerUserId("owner");when(pets.findById("pet")).thenReturn(Optional.of(pet));
        when(appointments.save(any(Appointment.class))).thenAnswer(call->call.getArgument(0));
        var service=new AppointmentSlotService(slots,appointments,pets,new IdGenerator());
        var result=service.bookSlotAtomically("owner",new BookSlotRequest("slot","pet","Checkup",null));
        assertEquals("pet",result.getPetId());assertEquals("hospital",result.getHospitalId());assertEquals("vet",result.getVetUserId());assertEquals("BOOKED",result.getStatus());
        assertEquals(1,slot.getBookedCount());assertEquals("BOOKED",slot.getStatus());
    }
    private AppointmentSlot slot(LocalDate date) {
        var slot=new AppointmentSlot();slot.setSlotId("slot");slot.setHospitalId("hospital");slot.setVetUserId("vet");slot.setSlotDate(date);slot.setStartTime(LocalTime.of(10,0));slot.setEndTime(LocalTime.of(10,30));slot.setStatus("AVAILABLE");slot.setCapacity(1);slot.setBookedCount(0);slot.setSlotType("OPD_CONSULTATION");return slot;
    }
}
