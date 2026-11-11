package com.pet.vetalya.medshop.user.service;

import com.pet.vetalya.medshop.audit.service.StoreAuditService;
import com.pet.vetalya.medshop.auth.service.StoreTokenService;
import com.pet.vetalya.medshop.common.MedShopException;
import com.pet.vetalya.medshop.security.StoreUserStatusCache;
import com.pet.vetalya.medshop.user.dto.UserDtos.*;
import com.pet.vetalya.medshop.user.entity.StoreUser;
import com.pet.vetalya.medshop.user.repository.StoreUserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service
public class StoreUserService {

    private static final Set<String> ALLOWED_ROLES = Set.of("OWNER", "PHARMACIST", "CASHIER");
    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of("image/jpeg", "image/png", "image/webp");

    private final StoreUserRepository userRepo;
    private final StoreTokenService tokenService;
    private final PasswordEncoder passwordEncoder;
    private final StoreAuditService auditService;
    private final StoreUserStatusCache userStatusCache;

    @Value("${app.store.s3.bucket:medshop-uploads}")
    private String s3Bucket;

    @Value("${app.store.s3.region:ap-south-1}")
    private String s3Region;

    public StoreUserService(
            StoreUserRepository userRepo,
            StoreTokenService tokenService,
            PasswordEncoder passwordEncoder,
            StoreAuditService auditService,
            StoreUserStatusCache userStatusCache) {
        this.userRepo = userRepo;
        this.tokenService = tokenService;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
        this.userStatusCache = userStatusCache;
    }

    @Transactional
    public StoreUserResponse createStaff(String tenantId, String currentUserId, CreateStaffRequest req, String ipAddress) {
        String role = req.role().trim().toUpperCase();
        if (!ALLOWED_ROLES.contains(role)) {
            throw new MedShopException.BadRequestException("Invalid role. Allowed roles: " + ALLOWED_ROLES);
        }

        String email = req.email().trim().toLowerCase();
        String phone = req.phone().trim();

        if (userRepo.existsByEmail(email)) {
            throw new MedShopException.ConflictException("Email is already registered");
        }
        if (userRepo.existsByPhone(phone)) {
            throw new MedShopException.ConflictException("Phone number is already registered");
        }

        String username = (req.username() != null && !req.username().isBlank())
                ? req.username().trim()
                : generateUniqueUsername(email);

        if (userRepo.existsByUsername(username)) {
            throw new MedShopException.ConflictException("Username is already taken");
        }

        StoreUser user = new StoreUser();
        user.setTenantId(tenantId);
        user.setUsername(username);
        user.setEmail(email);
        user.setPhone(phone);
        user.setFirstName(req.firstName().trim());
        user.setMiddleName(req.middleName() != null ? req.middleName().trim() : null);
        user.setLastName(req.lastName().trim());
        user.setRole(role);
        user.setPasswordHash(req.initialPassword() != null && !req.initialPassword().isBlank()
                ? passwordEncoder.encode(req.initialPassword()) : null);
        user.setActive(true);
        user.setTokenVersion(0);

        user = userRepo.save(user);

        auditService.record(tenantId, currentUserId, "USER", user.getId(), "CREATE_STAFF", null, mapToDto(user), ipAddress);
        return mapToDto(user);
    }

    @Transactional(readOnly = true)
    public Page<StoreUserResponse> getUsers(String tenantId, Pageable pageable) {
        return userRepo.findByTenantId(tenantId, pageable).map(u -> mapToDto(u));
    }

    @Transactional(readOnly = true)
    public StoreUserResponse getUser(String tenantId, String id) {
        StoreUser user = userRepo.findByTenantIdAndId(tenantId, id)
                .orElseThrow(() -> new MedShopException.NotFoundException("User not found"));
        return mapToDto(user);
    }

    @Transactional
    public StoreUserResponse updateUser(String tenantId, String id, UpdateUserRequest req) {
        StoreUser user = userRepo.findByTenantIdAndId(tenantId, id)
                .orElseThrow(() -> new MedShopException.NotFoundException("User not found"));

        user.setFirstName(req.firstName().trim());
        user.setMiddleName(req.middleName() != null ? req.middleName().trim() : null);
        user.setLastName(req.lastName().trim());
        user.setAddress(req.address());
        user.setProfilePic(req.profilePic());

        user = userRepo.save(user);
        return mapToDto(user);
    }

    @Transactional
    public StoreUserResponse changeActive(String tenantId, String currentUserId, String targetUserId, boolean active, String ipAddress) {
        StoreUser user = userRepo.findByTenantIdAndId(tenantId, targetUserId)
                .orElseThrow(() -> new MedShopException.NotFoundException("User not found"));

        if (Objects.equals(currentUserId, targetUserId)) {
            throw new MedShopException.ConflictException("Cannot activate or deactivate your own account");
        }

        if (!active && "OWNER".equals(user.getRole())) {
            long ownerCount = userRepo.countByTenantIdAndRoleAndActiveTrue(tenantId, "OWNER");
            if (ownerCount <= 1) {
                throw new MedShopException.ConflictException("Cannot deactivate the last active OWNER");
            }
        }

        user.setActive(active);
        user.setTokenVersion(user.getTokenVersion() + 1); // Bump token version
        user = userRepo.save(user);

        tokenService.revokeAllForUser(tenantId, targetUserId);
        userStatusCache.evict(targetUserId);

        auditService.record(tenantId, currentUserId, "USER", targetUserId,
                active ? "ACTIVATE_USER" : "DEACTIVATE_USER", null, mapToDto(user), ipAddress);

        return mapToDto(user);
    }

    @Transactional
    public StoreUserResponse changeRole(String tenantId, String currentUserId, String targetUserId, String newRole, String ipAddress) {
        String cleanRole = newRole.trim().toUpperCase();
        if (!ALLOWED_ROLES.contains(cleanRole)) {
            throw new MedShopException.BadRequestException("Invalid role. Allowed roles: " + ALLOWED_ROLES);
        }

        StoreUser user = userRepo.findByTenantIdAndId(tenantId, targetUserId)
                .orElseThrow(() -> new MedShopException.NotFoundException("User not found"));

        if (Objects.equals(currentUserId, targetUserId)) {
            throw new MedShopException.ConflictException("Cannot demote or change your own role");
        }

        if ("OWNER".equals(user.getRole()) && !"OWNER".equals(cleanRole)) {
            long ownerCount = userRepo.countByTenantIdAndRoleAndActiveTrue(tenantId, "OWNER");
            if (ownerCount <= 1) {
                throw new MedShopException.ConflictException("Cannot demote the last active OWNER");
            }
        }

        String oldRole = user.getRole();
        user.setRole(cleanRole);
        user.setTokenVersion(user.getTokenVersion() + 1); // Bump token version
        user = userRepo.save(user);

        tokenService.revokeAllForUser(tenantId, targetUserId);
        userStatusCache.evict(targetUserId);

        auditService.record(tenantId, currentUserId, "USER", targetUserId, "CHANGE_ROLE",
                "role=" + oldRole, "role=" + cleanRole, ipAddress);

        return mapToDto(user);
    }

    public PresignedUrlResponse generateProfilePicUploadUrl(String tenantId, String userId, String contentType) {
        String cleanType = contentType.trim().toLowerCase();
        if (!ALLOWED_IMAGE_TYPES.contains(cleanType)) {
            throw new MedShopException.BadRequestException(
                    "Only image uploads (JPEG, PNG, WEBP) are allowed. Provided: " + cleanType);
        }

        String ext = cleanType.equals("image/png") ? "png" : cleanType.equals("image/webp") ? "webp" : "jpg";
        String key = "tenants/" + tenantId + "/profiles/" + userId + "-" + UUID.randomUUID().toString().substring(0, 8) + "." + ext;

        String publicUrl = String.format("https://%s.s3.%s.amazonaws.com/%s", s3Bucket, s3Region, key);
        // Pre-signed S3 PUT URL mock (or standard signed URL format)
        String uploadUrl = String.format("https://%s.s3.%s.amazonaws.com/%s?X-Amz-Expires=900&mockSignature=true", s3Bucket, s3Region, key);

        return new PresignedUrlResponse(uploadUrl, publicUrl, key);
    }

    private String generateUniqueUsername(String base) {
        String prefix = base.contains("@") ? base.substring(0, base.indexOf('@')) : base;
        prefix = prefix.replaceAll("[^a-zA-Z0-9_]", "").toLowerCase();
        if (prefix.isBlank()) prefix = "user";
        String candidate = prefix;
        int count = 1;
        while (userRepo.existsByUsername(candidate)) {
            candidate = prefix + count++;
        }
        return candidate;
    }

    public StoreUserResponse mapToDto(StoreUser u) {
        return new StoreUserResponse(
                u.getId(), u.getTenantId(), u.getUsername(), u.getEmail(), u.getPhone(),
                u.getRole(), u.getFirstName(), u.getMiddleName(), u.getLastName(),
                u.getAddress(), u.getProfilePic(), u.isActive()
        );
    }
}

