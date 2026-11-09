package com.pet.vetalya.repository;

import com.pet.vetalya.entity.Vaccination;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface VaccinationRepository extends JpaRepository<Vaccination, String> {

    List<Vaccination> findAllByPetIdAndVetUserId(String petId, String vetUserId);

    Optional<Vaccination> findByVaccinationIdAndVetUserId(String vaccinationId, String vetUserId);

    /**
     * Vaccinations due (next_vaccination_date within range) for pets that this vet has treated.
     */
    @Query("""
            SELECT v FROM Vaccination v
            WHERE v.vetUserId = :vetUserId
              AND v.nextVaccinationDate IS NOT NULL
              AND v.nextVaccinationDate >= :from
              AND v.nextVaccinationDate <= :to
            ORDER BY v.nextVaccinationDate ASC
            """)
    List<Vaccination> findDueForVet(String vetUserId, LocalDate from, LocalDate to);

    // ── Owner queries ──

    @Query("""
            SELECT v, vt.firstName, vt.lastName FROM Vaccination v
            LEFT JOIN Vet vt ON v.vetUserId = vt.userId
            WHERE v.petId = :petId
            ORDER BY v.lastVaccinationDate DESC, v.createdAt DESC
            """)
    List<Object[]> findHistoryForPetWithVetName(String petId);

    @Query("""
            SELECT v.nextVaccinationDate FROM Vaccination v
            WHERE v.petId = :petId
              AND v.nextVaccinationDate >= :today
            ORDER BY v.nextVaccinationDate ASC
            """)
    List<LocalDate> findNextVaccinationDateForPet(String petId, LocalDate today, org.springframework.data.domain.Pageable pageable);

    @Query("""
            SELECT v, p.name, vt.firstName, vt.lastName FROM Vaccination v
            JOIN v.pet p
            LEFT JOIN Vet vt ON v.vetUserId = vt.userId
            WHERE p.ownerUserId = :ownerUserId
              AND p.deletedAt IS NULL
              AND v.nextVaccinationDate IS NOT NULL
              AND v.nextVaccinationDate >= :from
              AND v.nextVaccinationDate <= :to
            ORDER BY v.nextVaccinationDate ASC
            """)
    List<Object[]> findDueForOwner(String ownerUserId, LocalDate from, LocalDate to);

    @Query("""
            SELECT COUNT(v) FROM Vaccination v
            JOIN v.pet p
            WHERE p.ownerUserId = :ownerUserId
              AND p.deletedAt IS NULL
              AND v.nextVaccinationDate IS NOT NULL
              AND v.nextVaccinationDate >= :from
              AND v.nextVaccinationDate <= :to
            """)
    long countDueForOwner(String ownerUserId, LocalDate from, LocalDate to);
}
