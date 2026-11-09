package com.pet.vetalya.repository;

import com.pet.vetalya.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, String> {

    /**
     * Paginated reviews for a vet with optional rating and hospital filters.
     */
    @Query("""
            SELECT r FROM Review r
            JOIN FETCH r.owner o
            WHERE r.vetUserId = :vetUserId
              AND (:rating IS NULL OR r.rating = :rating)
              AND (:hospitalId IS NULL OR r.hospitalId = :hospitalId)
            """)
    Page<Review> findReviewsForVet(
            String vetUserId,
            Integer rating,
            String hospitalId,
            Pageable pageable);

    /**
     * Star-rating summary: count per star (1-5) for a vet.
     */
    @Query("""
            SELECT r.rating, COUNT(r) FROM Review r
            WHERE r.vetUserId = :vetUserId
            GROUP BY r.rating
            ORDER BY r.rating
            """)
    List<Object[]> countByRatingForVet(String vetUserId);

    /**
     * Average rating for a vet.
     */
    @Query("SELECT COALESCE(AVG(r.rating), 0) FROM Review r WHERE r.vetUserId = :vetUserId")
    double averageRatingForVet(String vetUserId);

    /**
     * Total review count for a vet.
     */
    long countByVetUserId(String vetUserId);

    /**
     * Average rating and review count per vet, for public search.
     */
    @Query("""
            SELECT r.vetUserId, AVG(r.rating), COUNT(r)
            FROM Review r
            WHERE r.vetUserId IN :vetUserIds
            GROUP BY r.vetUserId
            """)
    List<Object[]> avgRatingAndCountForVets(List<String> vetUserIds);

    // ── Owner queries ──

    boolean existsByAppointmentIdAndOwnerUserId(String appointmentId, String ownerUserId);

    Optional<Review> findByReviewIdAndOwnerUserId(String reviewId, String ownerUserId);

    Page<Review> findAllByOwnerUserId(String ownerUserId, Pageable pageable);
}
