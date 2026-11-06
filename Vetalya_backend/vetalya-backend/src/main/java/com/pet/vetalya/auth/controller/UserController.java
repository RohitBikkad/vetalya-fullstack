package com.pet.vetalya.auth.controller;

import com.pet.vetalya.entity.PetOwner;
import com.pet.vetalya.entity.UserAuth;
import com.pet.vetalya.entity.Vet;
import com.pet.vetalya.repository.PetOwnerRepository;
import com.pet.vetalya.repository.UserAuthRepository;
import com.pet.vetalya.repository.VetRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserAuthRepository userAuthRepo;
    private final PetOwnerRepository petOwnerRepo;
    private final VetRepository vetRepo;

    public UserController(UserAuthRepository userAuthRepo,
                          PetOwnerRepository petOwnerRepo,
                          VetRepository vetRepo) {
        this.userAuthRepo = userAuthRepo;
        this.petOwnerRepo = petOwnerRepo;
        this.vetRepo = vetRepo;
    }

    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(@AuthenticationPrincipal Jwt jwt) {
        if (jwt == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "unauthorized"));
        }
        String userId = jwt.getSubject();
        UserAuth user = userAuthRepo.findById(userId).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "user_not_found"));
        }

        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("id", user.getUserId());
        profile.put("email", user.getEmail());
        profile.put("role", user.getRole());
        profile.put("status", user.getStatus());
        profile.put("phone", user.getPhone());

        if ("ROLE_VET".equals(user.getRole())) {
            vetRepo.findById(userId).ifPresent(v -> {
                profile.put("firstName", v.getFirstName());
                profile.put("lastName", v.getLastName());
                profile.put("fullName", ((v.getFirstName() != null ? v.getFirstName() : "") + " " + (v.getLastName() != null ? v.getLastName() : "")).trim());
                profile.put("avatar", v.getPhotoUrl());
            });
        } else {
            petOwnerRepo.findById(userId).ifPresent(po -> {
                profile.put("firstName", po.getFirstName());
                profile.put("lastName", po.getLastName());
                profile.put("fullName", ((po.getFirstName() != null ? po.getFirstName() : "") + " " + (po.getLastName() != null ? po.getLastName() : "")).trim());
            });
        }

        return ResponseEntity.ok(profile);
    }

    @GetMapping("/vet-portal")
    public ResponseEntity<Map<String, Object>> getVeterinarianDashboard(@AuthenticationPrincipal Jwt jwt) {
        if (jwt == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "unauthorized"));
        }
        return ResponseEntity.ok(Map.of(
                "module", "Vetalya Clinical OPD Desk",
                "message", "Authorized access granted to clinical records and prescriptions."
        ));
    }

    @GetMapping("/admin-portal")
    public ResponseEntity<Map<String, Object>> getAdminDashboard(@AuthenticationPrincipal Jwt jwt) {
        if (jwt == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "unauthorized"));
        }
        return ResponseEntity.ok(Map.of(
                "module", "Vetalya Platform Compliance & Verification",
                "message", "Authorized access granted to council verification registry."
        ));
    }
}

