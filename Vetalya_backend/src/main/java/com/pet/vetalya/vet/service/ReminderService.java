package com.pet.vetalya.vet.service;

import com.pet.vetalya.common.PageResponse;
import com.pet.vetalya.entity.Reminder;
import com.pet.vetalya.repository.ReminderRepository;
import com.pet.vetalya.vet.dto.ReminderResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class ReminderService {

    private static final int MAX_PAGE_SIZE = 50;

    private final ReminderRepository reminderRepo;

    public ReminderService(ReminderRepository reminderRepo) {
        this.reminderRepo = reminderRepo;
    }

    @Transactional(readOnly = true)
    public PageResponse<ReminderResponse> listUpcoming(String vetUserId, int page, int size) {
        size = Math.min(size, MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(page, size);

        Page<Reminder> reminders = reminderRepo.findUpcomingForVet(vetUserId, Instant.now(), pageable);
        Page<ReminderResponse> responsePage = reminders.map(r -> toResponse(r));
        return PageResponse.of(responsePage);
    }

    private ReminderResponse toResponse(Reminder r) {
        return new ReminderResponse(
                r.getReminderId(),
                r.getAppointmentId(),
                r.getRemindAt(),
                r.getMessage(),
                r.isSent(),
                r.getCreatedAt()
        );
    }
}
