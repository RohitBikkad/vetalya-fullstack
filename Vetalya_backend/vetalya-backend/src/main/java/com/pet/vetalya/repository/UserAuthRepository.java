package com.pet.vetalya.repository;

import com.pet.vetalya.entity.UserAuth;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserAuthRepository extends JpaRepository<UserAuth, String> {
    Optional<UserAuth> findByEmailAndEmailVerified(String email, boolean emailVerified);
    Optional<UserAuth> findByEmail(String email);
    Optional<UserAuth> findFirstByPhone(String phone);
    List<UserAuth> findAllByPhone(String phone);
    long countByRole(String role);
    long countByStatus(String status);
}
