package com.pet.vetalya.repository;

import com.pet.vetalya.entity.DiagnosticOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DiagnosticOrderRepository extends JpaRepository<DiagnosticOrder, String> {
    List<DiagnosticOrder> findByPetIdOrderByCreatedAtDesc(String petId);
    List<DiagnosticOrder> findByVetUserIdOrderByCreatedAtDesc(String vetUserId);
    List<DiagnosticOrder> findByHospitalIdOrderByCreatedAtDesc(String hospitalId);
    Optional<DiagnosticOrder> findByOrderNumber(String orderNumber);
}
