package com.pet.vetalya.vet.controller;

import com.pet.vetalya.vet.dto.VetSearchResult;
import com.pet.vetalya.vet.service.VetSearchService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Public vet search endpoints accessible by any authenticated user (VET or PET_OWNER).
 */
@RestController
@RequestMapping("/api/vets")
@PreAuthorize("isAuthenticated()")
public class VetSearchController {

    private final VetSearchService vetSearchService;

    public VetSearchController(VetSearchService vetSearchService) {
        this.vetSearchService = vetSearchService;
    }

    /**
     * GET /api/vets/search — Search verified vets.
     * Sort: same pincode first, then numerically nearest pincode, then highest average rating.
     */
    @GetMapping("/search")
    public List<VetSearchResult> search(@RequestParam(required = false) String pincode) {
        return vetSearchService.search(pincode);
    }

    /**
     * GET /api/vets/{vetUserId} — Public profile of one verified vet.
     */
    @GetMapping("/{vetUserId}")
    public VetSearchResult getPublicProfile(@PathVariable String vetUserId) {
        return vetSearchService.getPublicProfile(vetUserId);
    }
}
