package com.pet.vetalya.vet.service;

import com.pet.vetalya.common.GlobalExceptionHandler.ResourceNotFoundException;
import com.pet.vetalya.entity.Hospital;
import com.pet.vetalya.entity.Vet;
import com.pet.vetalya.repository.HospitalRepository;
import com.pet.vetalya.repository.ReviewRepository;
import com.pet.vetalya.repository.VetRepository;
import com.pet.vetalya.vet.dto.VetSearchResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class VetSearchService {

    private final VetRepository vetRepo;
    private final HospitalRepository hospitalRepo;
    private final ReviewRepository reviewRepo;
    private final com.pet.vetalya.repository.UserAuthRepository userAuthRepo;

    public VetSearchService(VetRepository vetRepo,
                            HospitalRepository hospitalRepo,
                            ReviewRepository reviewRepo,
                            com.pet.vetalya.repository.UserAuthRepository userAuthRepo) {
        this.vetRepo = vetRepo;
        this.hospitalRepo = hospitalRepo;
        this.reviewRepo = reviewRepo;
        this.userAuthRepo = userAuthRepo;
    }

    /**
     * Search verified vets. Sort: same pincode first, then nearest pincode, then highest avg rating.
     */
    @Transactional(readOnly = true)
    public List<VetSearchResult> search(String pincode) {
        // Get all verified vets who are ACTIVE
        List<Vet> verifiedVets = vetRepo.findAll().stream()
                .filter(Vet::isVerified)
                .filter(v -> userAuthRepo.findById(v.getUserId())
                        .map(u -> "ACTIVE".equals(u.getStatus()))
                        .orElse(false))
                .toList();

        if (verifiedVets.isEmpty()) {
            return List.of();
        }

        List<String> vetUserIds = verifiedVets.stream().map(Vet::getUserId).toList();

        // Batch-load active hospitals for these vets
        Map<String, List<Hospital>> hospitalsByVet = new HashMap<>();
        for (Vet vet : verifiedVets) {
            hospitalsByVet.put(vet.getUserId(), hospitalRepo.findAllByVetUserIdAndActiveTrue(vet.getUserId()));
        }

        // Batch-load ratings
        Map<String, double[]> ratingMap = new HashMap<>(); // userId -> [avgRating, count]
        List<Object[]> ratingData = reviewRepo.avgRatingAndCountForVets(vetUserIds);
        for (Object[] row : ratingData) {
            ratingMap.put((String) row[0], new double[]{(double) row[1], ((long) row[2])});
        }

        // Build results
        List<VetSearchResult> results = new ArrayList<>();
        for (Vet vet : verifiedVets) {
            double[] rc = ratingMap.getOrDefault(vet.getUserId(), new double[]{0, 0});
            List<Hospital> hospitals = hospitalsByVet.getOrDefault(vet.getUserId(), List.of());

            List<VetSearchResult.HospitalSummary> hospitalSummaries = hospitals.stream()
                    .map(h -> new VetSearchResult.HospitalSummary(
                            h.getHospitalId(), h.getName(), h.getAddress1(), h.getAddress2(),
                            h.getCity(), h.getState(), h.getPincode(), h.getCountry(),
                            h.getMobile(), h.isIs24x7(), h.getOpenTime(), h.getCloseTime()))
                    .toList();

            results.add(new VetSearchResult(
                    vet.getUserId(), vet.getFirstName(), vet.getLastName(), vet.getPhotoUrl(),
                    Math.round(rc[0] * 10.0) / 10.0, (long) rc[1], hospitalSummaries));
        }

        // Sort: same pincode first, then numerically nearest pincode, then highest avg rating
        if (pincode != null && !pincode.isBlank()) {
            int searchPincode = parsePincode(pincode);
            results.sort((a, b) -> {
                int pinA = closestPincode(a.hospitals(), searchPincode);
                int pinB = closestPincode(b.hospitals(), searchPincode);
                int distA = Math.abs(pinA - searchPincode);
                int distB = Math.abs(pinB - searchPincode);
                if (distA != distB) return Integer.compare(distA, distB);
                return Double.compare(b.averageRating(), a.averageRating()); // higher rating first
            });
        } else {
            results.sort(Comparator.comparingDouble(VetSearchResult::averageRating).reversed());
        }

        return results;
    }

    /**
     * Get public profile of one verified vet.
     */
    @Transactional(readOnly = true)
    public VetSearchResult getPublicProfile(String vetUserId) {
        Vet vet = vetRepo.findById(vetUserId)
                .filter(Vet::isVerified)
                .orElseThrow(() -> new ResourceNotFoundException("Vet not found or not verified"));

        userAuthRepo.findById(vetUserId)
                .filter(u -> "ACTIVE".equals(u.getStatus()))
                .orElseThrow(() -> new ResourceNotFoundException("Vet not found or not active"));

        List<Hospital> hospitals = hospitalRepo.findAllByVetUserIdAndActiveTrue(vetUserId);
        List<Object[]> ratingData = reviewRepo.avgRatingAndCountForVets(List.of(vetUserId));
        double avgRating = 0;
        long reviewCount = 0;
        if (!ratingData.isEmpty()) {
            avgRating = Math.round((double) ratingData.get(0)[1] * 10.0) / 10.0;
            reviewCount = (long) ratingData.get(0)[2];
        }

        List<VetSearchResult.HospitalSummary> hospitalSummaries = hospitals.stream()
                .map(h -> new VetSearchResult.HospitalSummary(
                        h.getHospitalId(), h.getName(), h.getAddress1(), h.getAddress2(),
                        h.getCity(), h.getState(), h.getPincode(), h.getCountry(),
                        h.getMobile(), h.isIs24x7(), h.getOpenTime(), h.getCloseTime()))
                .toList();

        return new VetSearchResult(
                vet.getUserId(), vet.getFirstName(), vet.getLastName(), vet.getPhotoUrl(),
                avgRating, reviewCount, hospitalSummaries);
    }

    private int parsePincode(String pincode) {
        try {
            return Integer.parseInt(pincode);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private int closestPincode(List<VetSearchResult.HospitalSummary> hospitals, int target) {
        if (hospitals.isEmpty()) return Integer.MAX_VALUE;
        return hospitals.stream()
                .mapToInt(h -> {
                    try { return Integer.parseInt(h.pincode()); }
                    catch (NumberFormatException e) { return Integer.MAX_VALUE; }
                })
                .min()
                .orElse(Integer.MAX_VALUE);
    }
}
