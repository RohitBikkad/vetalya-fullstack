package com.pet.vetalya.repository;

import com.pet.vetalya.entity.PetDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface PetDocumentRepository extends JpaRepository<PetDocument, String> {

    List<PetDocument> findAllByPetIdAndIsDeletedIsFalse(String petId);

    Page<PetDocument> findAllByPetIdAndIsDeletedIsFalse(String petId, Pageable pageable);

    Page<PetDocument> findAllByPetIdAndCategoryAndIsDeletedIsFalse(String petId, String category, Pageable pageable);

    Optional<PetDocument> findByDocumentIdAndIsDeletedIsFalse(String documentId);
}
