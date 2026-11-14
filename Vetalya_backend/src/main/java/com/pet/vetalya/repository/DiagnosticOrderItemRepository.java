package com.pet.vetalya.repository;

import com.pet.vetalya.entity.DiagnosticOrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DiagnosticOrderItemRepository extends JpaRepository<DiagnosticOrderItem, String> {
    List<DiagnosticOrderItem> findByOrder_OrderId(String orderId);
}
