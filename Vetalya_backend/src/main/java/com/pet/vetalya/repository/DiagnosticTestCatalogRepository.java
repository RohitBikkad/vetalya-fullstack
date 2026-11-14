package com.pet.vetalya.repository;

import com.pet.vetalya.entity.DiagnosticTestCatalog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DiagnosticTestCatalogRepository extends JpaRepository<DiagnosticTestCatalog, String> {
    Optional<DiagnosticTestCatalog> findByTestCode(String testCode);
    List<DiagnosticTestCatalog> findByCategoryOrderByTestNameAsc(String category);
}
