package com.pet.vetalya.medshop.billing.repository;

import com.pet.vetalya.medshop.billing.entity.InvoiceItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InvoiceItemRepository extends JpaRepository<InvoiceItem, String> {

    List<InvoiceItem> findByTenantIdAndInvoiceId(String tenantId, String invoiceId);
}

