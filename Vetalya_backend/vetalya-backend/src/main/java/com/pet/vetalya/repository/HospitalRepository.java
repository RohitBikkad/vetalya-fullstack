package com.pet.vetalya.repository;

import com.pet.vetalya.entity.Hospital;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface HospitalRepository extends JpaRepository<Hospital, String> {

    List<Hospital> findAllByVetUserId(String vetUserId);

    List<Hospital> findAllByVetUserIdAndActiveTrue(String vetUserId);

    Optional<Hospital> findByHospitalIdAndVetUserId(String hospitalId, String vetUserId);

    Optional<Hospital> findByHospitalIdAndVetUserIdAndActiveTrue(String hospitalId, String vetUserId);

    long countByVetUserIdAndActiveTrue(String vetUserId);

    @Query("SELECT h FROM Hospital h WHERE h.vetUserId = :vetUserId AND h.hospitalId IN :ids")
    List<Hospital> findAllByVetUserIdAndHospitalIdIn(String vetUserId, List<String> ids);
}
