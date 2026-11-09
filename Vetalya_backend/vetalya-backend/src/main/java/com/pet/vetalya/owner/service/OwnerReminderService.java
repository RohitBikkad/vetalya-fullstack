package com.pet.vetalya.owner.service;

import com.pet.vetalya.common.PageResponse;
import com.pet.vetalya.entity.Reminder;
import com.pet.vetalya.owner.dto.OwnerReminderResponse;
import com.pet.vetalya.repository.ReminderRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class OwnerReminderService {

    private final ReminderRepository reminderRepo;

    public OwnerReminderService(ReminderRepository reminderRepo) {
        this.reminderRepo = reminderRepo;
    }

    @Transactional(readOnly = true)
    public PageResponse<OwnerReminderResponse> listUpcoming(String ownerUserId, int page, int size) {
        int boundedSize = Math.min(Math.max(size, 1), 50);
        Pageable pageable = PageRequest.of(Math.max(page, 0), boundedSize);

        Page<Reminder> reminderPage = reminderRepo.findUpcomingForUser(ownerUserId, Instant.now(), pageable);
        List<OwnerReminderResponse> content = reminderPage.getContent().stream()
                .map(r -> new OwnerReminderResponse(
                        r.getReminderId(),
                        r.getAppointmentId(),
                        r.getRemindAt(),
                        r.getMessage(),
                        r.getPetId(),
                        r.getCreatedAt()))
                .toList();

        return new PageResponse<>(content, reminderPage.getNumber(), reminderPage.getSize(),
                reminderPage.getTotalElements(), reminderPage.getTotalPages());
    }
}
