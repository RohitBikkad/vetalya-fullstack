package com.pet.vetalya.owner.controller;

import com.pet.vetalya.common.PageResponse;
import com.pet.vetalya.owner.dto.OwnerReviewResponse;
import com.pet.vetalya.owner.dto.ReviewRequest;
import com.pet.vetalya.owner.service.OwnerReviewService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@PreAuthorize("hasRole('PET_OWNER')")
public class OwnerReviewController {

    private final OwnerReviewService reviewService;

    public OwnerReviewController(OwnerReviewService reviewService) {
        this.reviewService = reviewService;
    }

    /**
     * POST /api/owner/appointments/{id}/review — Submit review for a COMPLETED appointment.
     */
    @PostMapping("/api/owner/appointments/{id}/review")
    public ResponseEntity<OwnerReviewResponse> createReview(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String id,
            @Valid @RequestBody ReviewRequest request) {
        OwnerReviewResponse response = reviewService.createReview(jwt.getSubject(), id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * GET /api/owner/reviews — Paginated list of my reviews.
     */
    @GetMapping("/api/owner/reviews")
    public PageResponse<OwnerReviewResponse> listReviews(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return reviewService.listReviews(jwt.getSubject(), page, size);
    }

    /**
     * PUT /api/owner/reviews/{reviewId} — Update my review within edit window.
     */
    @PutMapping("/api/owner/reviews/{reviewId}")
    public OwnerReviewResponse updateReview(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String reviewId,
            @Valid @RequestBody ReviewRequest request) {
        return reviewService.updateReview(jwt.getSubject(), reviewId, request);
    }

    /**
     * DELETE /api/owner/reviews/{reviewId} — Delete my review.
     */
    @DeleteMapping("/api/owner/reviews/{reviewId}")
    public ResponseEntity<Void> deleteReview(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String reviewId) {
        reviewService.deleteReview(jwt.getSubject(), reviewId);
        return ResponseEntity.noContent().build();
    }
}
