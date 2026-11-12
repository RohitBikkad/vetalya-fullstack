package com.pet.vetalya.vet.controller;

import com.pet.vetalya.common.PageResponse;
import com.pet.vetalya.vet.dto.ReviewResponse;
import com.pet.vetalya.vet.dto.ReviewSummaryResponse;
import com.pet.vetalya.vet.service.ReviewService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

/**
 * Read-only review endpoints for vets.
 */
@RestController
@RequestMapping("/api/vet/reviews")
@PreAuthorize("hasRole('VET')")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    /**
     * GET /api/vet/reviews — Paginated reviews with optional rating and hospital filters.
     */
    @GetMapping
    public PageResponse<ReviewResponse> list(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) Integer rating,
            @RequestParam(required = false) String hospitalId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return reviewService.listReviews(jwt.getSubject(), rating, hospitalId, page, size);
    }

    /**
     * GET /api/vet/reviews/summary — Star-rating summary with counts per star and average.
     */
    @GetMapping("/summary")
    public ReviewSummaryResponse summary(@AuthenticationPrincipal Jwt jwt) {
        return reviewService.getSummary(jwt.getSubject());
    }
}
