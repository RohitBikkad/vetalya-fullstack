package com.pet.vetalya.vet.service;

import com.pet.vetalya.common.PageResponse;
import com.pet.vetalya.entity.PetOwner;
import com.pet.vetalya.entity.Review;
import com.pet.vetalya.repository.ReviewRepository;
import com.pet.vetalya.vet.dto.ReviewResponse;
import com.pet.vetalya.vet.dto.ReviewSummaryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ReviewService {

    private static final int MAX_PAGE_SIZE = 50;

    private final ReviewRepository reviewRepo;

    public ReviewService(ReviewRepository reviewRepo) {
        this.reviewRepo = reviewRepo;
    }

    @Transactional(readOnly = true)
    public PageResponse<ReviewResponse> listReviews(
            String vetUserId, Integer rating, String hospitalId, int page, int size) {

        size = Math.min(size, MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Page<Review> reviews = reviewRepo.findReviewsForVet(vetUserId, rating, hospitalId, pageable);
        Page<ReviewResponse> responsePage = reviews.map(this::toResponse);
        return PageResponse.of(responsePage);
    }

    @Transactional(readOnly = true)
    public ReviewSummaryResponse getSummary(String vetUserId) {
        List<Object[]> starCounts = reviewRepo.countByRatingForVet(vetUserId);
        Map<Integer, Long> counts = new LinkedHashMap<>();
        long totalReviews = 0;
        for (Object[] row : starCounts) {
            int star = (int) row[0];
            long count = (long) row[1];
            counts.put(star, count);
            totalReviews += count;
        }
        double avgRating = totalReviews > 0 ? reviewRepo.averageRatingForVet(vetUserId) : 0;
        return new ReviewSummaryResponse(counts, totalReviews, avgRating);
    }

    private ReviewResponse toResponse(Review r) {
        PetOwner owner = r.getOwner();
        return new ReviewResponse(
                r.getReviewId(),
                r.getHospitalId(),
                r.getRating(),
                r.getComment(),
                owner != null ? owner.getFirstName() : null,
                owner != null ? owner.getLastName() : null,
                r.getCreatedAt()
        );
    }
}
