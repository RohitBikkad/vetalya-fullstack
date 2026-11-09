package com.pet.vetalya.owner.service;

import com.pet.vetalya.common.GlobalExceptionHandler.ConflictException;
import com.pet.vetalya.common.GlobalExceptionHandler.ResourceNotFoundException;
import com.pet.vetalya.common.IdGenerator;
import com.pet.vetalya.common.PageResponse;
import com.pet.vetalya.config.AppProperties;
import com.pet.vetalya.entity.Appointment;
import com.pet.vetalya.entity.Hospital;
import com.pet.vetalya.entity.Review;
import com.pet.vetalya.entity.Vet;
import com.pet.vetalya.owner.dto.OwnerReviewResponse;
import com.pet.vetalya.owner.dto.ReviewRequest;
import com.pet.vetalya.repository.AppointmentRepository;
import com.pet.vetalya.repository.HospitalRepository;
import com.pet.vetalya.repository.ReviewRepository;
import com.pet.vetalya.repository.VetRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
public class OwnerReviewService {

    private static final Logger log = LoggerFactory.getLogger(OwnerReviewService.class);

    private final ReviewRepository reviewRepo;
    private final AppointmentRepository appointmentRepo;
    private final VetRepository vetRepo;
    private final HospitalRepository hospitalRepo;
    private final IdGenerator idGenerator;
    private final AppProperties appProperties;

    public OwnerReviewService(ReviewRepository reviewRepo,
                              AppointmentRepository appointmentRepo,
                              VetRepository vetRepo,
                              HospitalRepository hospitalRepo,
                              IdGenerator idGenerator,
                              AppProperties appProperties) {
        this.reviewRepo = reviewRepo;
        this.appointmentRepo = appointmentRepo;
        this.vetRepo = vetRepo;
        this.hospitalRepo = hospitalRepo;
        this.idGenerator = idGenerator;
        this.appProperties = appProperties;
    }

    @Transactional
    public OwnerReviewResponse createReview(String ownerUserId, String appointmentId, ReviewRequest request) {
        Appointment apt = appointmentRepo.findByAppointmentIdAndOwnerUserId(appointmentId, ownerUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found"));

        if (!"COMPLETED".equals(apt.getStatus())) {
            throw new ConflictException("Reviews can only be submitted for COMPLETED appointments");
        }

        if (reviewRepo.existsByAppointmentIdAndOwnerUserId(appointmentId, ownerUserId)) {
            throw new ConflictException("You have already reviewed this appointment");
        }

        Review review = new Review();
        review.setReviewId(idGenerator.newId());
        review.setAppointmentId(appointmentId);
        review.setOwnerUserId(ownerUserId);
        review.setVetUserId(apt.getVetUserId());
        review.setHospitalId(apt.getHospitalId() != null ? apt.getHospitalId() : "");
        review.setRating(request.rating());
        review.setTitle(request.title());
        review.setComment(request.comment());

        review = reviewRepo.save(review);
        log.info("Review created: reviewId={}, appointmentId={}, ownerUserId={}",
                review.getReviewId(), appointmentId, ownerUserId);

        return toResponse(review);
    }

    @Transactional(readOnly = true)
    public PageResponse<OwnerReviewResponse> listReviews(String ownerUserId, int page, int size) {
        int boundedSize = Math.min(Math.max(size, 1), 50);
        Pageable pageable = PageRequest.of(Math.max(page, 0), boundedSize, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<Review> reviewPage = reviewRepo.findAllByOwnerUserId(ownerUserId, pageable);
        List<OwnerReviewResponse> content = reviewPage.getContent().stream()
                .map(this::toResponse)
                .toList();

        return new PageResponse<>(content, reviewPage.getNumber(), reviewPage.getSize(),
                reviewPage.getTotalElements(), reviewPage.getTotalPages());
    }

    @Transactional
    public OwnerReviewResponse updateReview(String ownerUserId, String reviewId, ReviewRequest request) {
        Review review = reviewRepo.findByReviewIdAndOwnerUserId(reviewId, ownerUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found"));

        int editWindowDays = appProperties.review().editWindowDays();
        Instant deadline = review.getCreatedAt().plus(Duration.ofDays(editWindowDays));
        if (Instant.now().isAfter(deadline)) {
            throw new ConflictException("Review edit window of " + editWindowDays + " days has expired");
        }

        review.setTitle(request.title());
        review.setComment(request.comment());
        review.setRating(request.rating());
        review.setUpdatedAt(Instant.now());

        review = reviewRepo.save(review);
        log.info("Review updated: reviewId={}, ownerUserId={}", reviewId, ownerUserId);

        return toResponse(review);
    }

    @Transactional
    public void deleteReview(String ownerUserId, String reviewId) {
        Review review = reviewRepo.findByReviewIdAndOwnerUserId(reviewId, ownerUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found"));

        reviewRepo.delete(review);
        log.info("Review deleted: reviewId={}, ownerUserId={}", reviewId, ownerUserId);
    }

    private OwnerReviewResponse toResponse(Review r) {
        Vet vet = vetRepo.findById(r.getVetUserId()).orElse(null);
        String vetName = vet != null ? (vet.getFirstName() + " " + (vet.getLastName() != null ? vet.getLastName() : "")).trim() : "Unknown Vet";

        String hospitalName = "N/A";
        if (r.getHospitalId() != null && !r.getHospitalId().isBlank()) {
            Hospital h = hospitalRepo.findById(r.getHospitalId()).orElse(null);
            if (h != null) hospitalName = h.getName();
        }

        int editWindowDays = appProperties.review().editWindowDays();
        boolean canEdit = r.getCreatedAt().plus(Duration.ofDays(editWindowDays)).isAfter(Instant.now());

        return new OwnerReviewResponse(
                r.getReviewId(),
                r.getAppointmentId(),
                r.getVetUserId(),
                vetName,
                r.getHospitalId(),
                hospitalName,
                r.getRating(),
                r.getTitle(),
                r.getComment(),
                canEdit,
                r.getCreatedAt(),
                r.getUpdatedAt()
        );
    }
}
