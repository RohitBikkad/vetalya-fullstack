package com.pet.vetalya.service;

import com.pet.vetalya.common.GlobalExceptionHandler.ResourceNotFoundException;
import com.pet.vetalya.common.IdGenerator;
import com.pet.vetalya.dto.*;
import com.pet.vetalya.entity.*;
import com.pet.vetalya.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.Year;
import java.util.List;

@Service
public class DiagnosticService {

    private static final Logger log = LoggerFactory.getLogger(DiagnosticService.class);

    private final DiagnosticOrderRepository orderRepo;
    private final DiagnosticOrderItemRepository itemRepo;
    private final DiagnosticTestCatalogRepository catalogRepo;
    private final PetRepository petRepo;
    private final IdGenerator idGenerator;

    public DiagnosticService(DiagnosticOrderRepository orderRepo,
                             DiagnosticOrderItemRepository itemRepo,
                             DiagnosticTestCatalogRepository catalogRepo,
                             PetRepository petRepo,
                             IdGenerator idGenerator) {
        this.orderRepo = orderRepo;
        this.itemRepo = itemRepo;
        this.catalogRepo = catalogRepo;
        this.petRepo = petRepo;
        this.idGenerator = idGenerator;
    }

    @Transactional
    public DiagnosticOrderResponse createOrder(String vetUserId, DiagnosticOrderRequest req) {
        petRepo.findById(req.petId())
                .orElseThrow(() -> new ResourceNotFoundException("Pet not found: " + req.petId()));

        String orderId = idGenerator.newId();
        String orderNumber = "DX-" + Year.now().getValue() + "-" + orderId.substring(orderId.length() - 6).toUpperCase();

        DiagnosticOrder order = new DiagnosticOrder();
        order.setOrderId(orderId);
        order.setOrderNumber(orderNumber);
        order.setPetId(req.petId());
        order.setVetUserId(vetUserId);
        order.setHospitalId(req.hospitalId());
        order.setEncounterId(req.encounterId());
        order.setStatus("ORDERED");
        order.setPriority(req.priority() != null ? req.priority() : "ROUTINE");
        order.setClinicalNotes(req.clinicalNotes());

        for (DiagnosticItemRequest itemReq : req.items()) {
            DiagnosticOrderItem item = new DiagnosticOrderItem();
            item.setItemId(idGenerator.newId());
            item.setTestCode(itemReq.testCode());
            item.setTestName(itemReq.testName());
            item.setCategory(itemReq.category());
            item.setStatus("PENDING");
            item.setFlag("NORMAL");
            order.addItem(item);
        }

        DiagnosticOrder saved = orderRepo.save(order);
        log.info("Diagnostic order created {} for pet {}", saved.getOrderNumber(), req.petId());
        return mapToResponse(saved);
    }

    @Transactional
    public DiagnosticOrderResponse updateItemResult(String itemId, DiagnosticResultUpdateRequest req) {
        DiagnosticOrderItem item = itemRepo.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Diagnostic item not found: " + itemId));

        item.setResultValue(req.resultValue());
        if (req.referenceRange() != null) item.setReferenceRange(req.referenceRange());
        if (req.unit() != null) item.setUnit(req.unit());
        if (req.flag() != null) item.setFlag(req.flag());
        if (req.interpretation() != null) item.setInterpretation(req.interpretation());
        if (req.reportFileUrl() != null) item.setReportFileUrl(req.reportFileUrl());
        item.setVerifiedBy(req.verifiedBy());
        item.setVerifiedAt(Instant.now());
        item.setStatus("COMPLETED");

        DiagnosticOrder order = item.getOrder();

        // Check if all items in order are completed
        boolean allCompleted = order.getItems().stream().allMatch(i -> "COMPLETED".equals(i.getStatus()));
        if (allCompleted) {
            order.setStatus("COMPLETED");
        } else {
            order.setStatus("PROCESSING");
        }

        orderRepo.save(order);
        log.info("Diagnostic item {} updated with result flag {}", itemId, item.getFlag());
        return mapToResponse(order);
    }

    @Transactional(readOnly = true)
    public List<DiagnosticOrderResponse> getOrdersForPet(String petId) {
        return orderRepo.findByPetIdOrderByCreatedAtDesc(petId).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public DiagnosticOrderResponse getOrderById(String orderId) {
        DiagnosticOrder order = orderRepo.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Diagnostic order not found: " + orderId));
        return mapToResponse(order);
    }

    @Transactional(readOnly = true)
    public List<DiagnosticTestCatalog> getCatalog(String category) {
        if (category != null && !category.isBlank()) {
            return catalogRepo.findByCategoryOrderByTestNameAsc(category);
        }
        return catalogRepo.findAll();
    }

    private DiagnosticOrderResponse mapToResponse(DiagnosticOrder order) {
        List<DiagnosticOrderItemResponse> itemResponses = order.getItems().stream()
                .map(i -> new DiagnosticOrderItemResponse(
                        i.getItemId(),
                        i.getTestCode(),
                        i.getTestName(),
                        i.getCategory(),
                        i.getStatus(),
                        i.getResultValue(),
                        i.getReferenceRange(),
                        i.getUnit(),
                        i.getFlag(),
                        i.getReportFileUrl(),
                        i.getInterpretation(),
                        i.getVerifiedBy(),
                        i.getVerifiedAt()
                ))
                .toList();

        return new DiagnosticOrderResponse(
                order.getOrderId(),
                order.getOrderNumber(),
                order.getPetId(),
                order.getVetUserId(),
                order.getHospitalId(),
                order.getEncounterId(),
                order.getStatus(),
                order.getPriority(),
                order.getClinicalNotes(),
                itemResponses,
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }
}

