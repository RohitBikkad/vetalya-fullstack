package com.pet.vetalya.medshop.billing.repository;

import com.pet.vetalya.medshop.billing.entity.InvoiceSequence;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InvoiceSequenceRepository extends JpaRepository<InvoiceSequence, InvoiceSequence.InvoiceSequenceId> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM InvoiceSequence s WHERE s.tenantId = :tenantId AND s.financialYear = :financialYear")
    Optional<InvoiceSequence> findWithLock(
            @Param("tenantId") String tenantId,
            @Param("financialYear") String financialYear
    );

    Optional<InvoiceSequence> findByTenantIdAndFinancialYear(String tenantId, String financialYear);
}

