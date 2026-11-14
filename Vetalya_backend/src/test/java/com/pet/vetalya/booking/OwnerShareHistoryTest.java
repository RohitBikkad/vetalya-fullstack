package com.pet.vetalya.booking;

import com.pet.vetalya.common.GlobalExceptionHandler.ForbiddenException;
import com.pet.vetalya.entity.Pet;
import com.pet.vetalya.repository.*;
import com.pet.vetalya.service.PetPassportService;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OwnerShareHistoryTest {
    private final PetRepository pets = mock(PetRepository.class);
    private final RecordShareRequestRepository shares = mock(RecordShareRequestRepository.class);
    private final PetPassportService service = new PetPassportService(pets, null, null, shares, null, null, null, null, null);

    @Test void ownerCanReadPersistentHistory() {
        Pet pet = new Pet(); pet.setOwnerUserId("owner");
        when(pets.findById("pet")).thenReturn(Optional.of(pet));
        when(shares.findAllByPetId("pet")).thenReturn(List.of());
        assertTrue(service.listShareRequests("owner", "pet").isEmpty());
        verify(shares).findAllByPetId("pet");
    }

    @Test void anotherOwnerCannotReadSharingHistory() {
        Pet pet = new Pet(); pet.setOwnerUserId("owner");
        when(pets.findById("pet")).thenReturn(Optional.of(pet));
        assertThrows(ForbiddenException.class, () -> service.listShareRequests("other", "pet"));
        verifyNoInteractions(shares);
    }
}
