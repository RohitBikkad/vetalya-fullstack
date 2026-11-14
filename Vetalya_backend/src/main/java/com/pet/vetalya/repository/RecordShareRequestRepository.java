package com.pet.vetalya.repository;

import com.pet.vetalya.entity.RecordShareRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface RecordShareRequestRepository extends JpaRepository<RecordShareRequest, String> {

    List<RecordShareRequest> findAllByPetId(String petId);

    Page<RecordShareRequest> findAllByPetId(String petId, Pageable pageable);

    Page<RecordShareRequest> findAllByRequesterUserId(String requesterUserId, Pageable pageable);

    Optional<RecordShareRequest> findByRequestIdAndPetId(String requestId, String petId);
}
