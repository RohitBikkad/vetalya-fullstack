package com.pet.vetalya.medshop.catalog.service;

import com.pet.vetalya.medshop.audit.service.StoreAuditService;
import com.pet.vetalya.medshop.catalog.dto.CatalogDtos.*;
import com.pet.vetalya.medshop.catalog.entity.Category;
import com.pet.vetalya.medshop.catalog.entity.Medicine;
import com.pet.vetalya.medshop.catalog.entity.Supplier;
import com.pet.vetalya.medshop.catalog.repository.CategoryRepository;
import com.pet.vetalya.medshop.catalog.repository.MedicineRepository;
import com.pet.vetalya.medshop.catalog.repository.SupplierRepository;
import com.pet.vetalya.medshop.common.MedShopException;
import com.pet.vetalya.medshop.common.ValidationPatterns;
import com.pet.vetalya.medshop.stock.repository.StockBatchRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

@Service
public class CatalogService {

    private static final Logger log = LoggerFactory.getLogger(CatalogService.class);

    private final CategoryRepository categoryRepo;
    private final SupplierRepository supplierRepo;
    private final MedicineRepository medicineRepo;
    private final StockBatchRepository batchRepo;
    private final StoreAuditService auditService;
    private final ZoneId zoneId;
    private final Set<Double> allowedGstRates;

    public CatalogService(
            CategoryRepository categoryRepo,
            SupplierRepository supplierRepo,
            MedicineRepository medicineRepo,
            StockBatchRepository batchRepo,
            StoreAuditService auditService,
            @Value("${app.zone:Asia/Kolkata}") String zone,
            @Value("${app.gst.allowed-rates:0,0.1,0.25,3,5,12,18,28}") List<Double> allowedGstRatesList) {
        this.categoryRepo = categoryRepo;
        this.supplierRepo = supplierRepo;
        this.medicineRepo = medicineRepo;
        this.batchRepo = batchRepo;
        this.auditService = auditService;
        this.zoneId = ZoneId.of(zone);
        this.allowedGstRates = new HashSet<>(allowedGstRatesList != null ? allowedGstRatesList : ValidationPatterns.DEFAULT_ALLOWED_GST_RATES);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // CATEGORIES
    // ══════════════════════════════════════════════════════════════════════════

    @Transactional
    public CategoryDto createCategory(String tenantId, String userId, CreateCategoryRequest req, String ipAddress) {
        String cleanName = req.name().trim();
        if (categoryRepo.existsByTenantIdAndName(tenantId, cleanName)) {
            throw new MedShopException.ConflictException("Category with this name already exists");
        }

        if (req.parentId() != null) {
            categoryRepo.findByTenantIdAndId(tenantId, req.parentId())
                    .orElseThrow(() -> new MedShopException.NotFoundException("Parent category not found"));
        }

        Category cat = new Category();
        cat.setTenantId(tenantId);
        cat.setName(cleanName);
        cat.setDescription(req.description());
        cat.setParentId(req.parentId());
        cat = categoryRepo.save(cat);

        auditService.record(tenantId, userId, "CATEGORY", cat.getId(), "CREATE", null, cat, ipAddress);
        return mapCategory(cat, null);
    }

    @Transactional(readOnly = true)
    public List<CategoryDto> getCategories(String tenantId, boolean tree) {
        List<Category> all = categoryRepo.findByTenantId(tenantId);
        if (!tree) {
            return all.stream().map(c -> mapCategory(c, null)).toList();
        }
        // Build category hierarchy tree
        Map<String, List<Category>> childrenMap = new HashMap<>();
        List<Category> roots = new ArrayList<>();
        for (Category c : all) {
            if (c.getParentId() == null) {
                roots.add(c);
            } else {
                childrenMap.computeIfAbsent(c.getParentId(), k -> new ArrayList<>()).add(c);
            }
        }
        return roots.stream().map(r -> buildTree(r, childrenMap)).toList();
    }

    private CategoryDto buildTree(Category node, Map<String, List<Category>> childrenMap) {
        List<Category> children = childrenMap.getOrDefault(node.getId(), Collections.emptyList());
        List<CategoryDto> childDtos = children.stream().map(c -> buildTree(c, childrenMap)).toList();
        return mapCategory(node, childDtos);
    }

    @Transactional(readOnly = true)
    public CategoryDto getCategory(String tenantId, String id) {
        Category cat = categoryRepo.findByTenantIdAndId(tenantId, id)
                .orElseThrow(() -> new MedShopException.NotFoundException("Category not found"));
        return mapCategory(cat, null);
    }

    @Transactional
    public CategoryDto updateCategory(String tenantId, String userId, String id, UpdateCategoryRequest req, String ipAddress) {
        Category cat = categoryRepo.findByTenantIdAndId(tenantId, id)
                .orElseThrow(() -> new MedShopException.NotFoundException("Category not found"));

        String cleanName = req.name().trim();
        if (!cat.getName().equalsIgnoreCase(cleanName) && categoryRepo.existsByTenantIdAndName(tenantId, cleanName)) {
            throw new MedShopException.ConflictException("Category with this name already exists");
        }

        if (req.parentId() != null) {
            if (req.parentId().equals(id)) {
                throw new MedShopException.ConflictException("Category cannot be its own parent");
            }
            // Cycle detection
            String currParent = req.parentId();
            while (currParent != null) {
                if (currParent.equals(id)) {
                    throw new MedShopException.ConflictException("Cyclic parent relationship detected");
                }
                Category parentCat = categoryRepo.findByTenantIdAndId(tenantId, currParent).orElse(null);
                currParent = parentCat != null ? parentCat.getParentId() : null;
            }
        }

        cat.setName(cleanName);
        cat.setDescription(req.description());
        cat.setParentId(req.parentId());
        cat = categoryRepo.save(cat);

        auditService.record(tenantId, userId, "CATEGORY", id, "UPDATE", null, cat, ipAddress);
        return mapCategory(cat, null);
    }

    @Transactional
    public void deleteCategory(String tenantId, String userId, String id, String ipAddress) {
        Category cat = categoryRepo.findByTenantIdAndId(tenantId, id)
                .orElseThrow(() -> new MedShopException.NotFoundException("Category not found"));

        if (categoryRepo.existsByTenantIdAndParentId(tenantId, id)) {
            throw new MedShopException.ConflictException("Cannot delete category with sub-categories. Remove or reassign them first.");
        }
        if (medicineRepo.existsByTenantIdAndCategoryId(tenantId, id)) {
            throw new MedShopException.ConflictException("Cannot delete category referenced by medicines. Reassign them first.");
        }

        categoryRepo.delete(cat);
        auditService.record(tenantId, userId, "CATEGORY", id, "DELETE", cat, null, ipAddress);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // SUPPLIERS
    // ══════════════════════════════════════════════════════════════════════════

    @Transactional
    public SupplierDto createSupplier(String tenantId, String userId, CreateSupplierRequest req, String ipAddress) {
        String cleanName = req.name().trim();
        if (supplierRepo.existsByTenantIdAndName(tenantId, cleanName)) {
            throw new MedShopException.ConflictException("Supplier with this name already exists");
        }

        if (!ValidationPatterns.isValidPhone(req.phone())) {
            throw new MedShopException.BadRequestException("Invalid phone number format: " + req.phone());
        }

        if (req.gstin() != null && !req.gstin().isBlank() && !ValidationPatterns.isValidGstin(req.gstin().trim().toUpperCase())) {
            throw new MedShopException.BadRequestException("Invalid GSTIN format: " + req.gstin());
        }

        Supplier s = new Supplier();
        s.setTenantId(tenantId);
        s.setName(cleanName);
        s.setContactPerson(req.contactPerson());
        s.setPhone(req.phone().trim());
        s.setGstin(req.gstin() != null ? req.gstin().trim().toUpperCase() : null);
        s.setAddress(req.address());
        s.setActive(true);
        s = supplierRepo.save(s);

        auditService.record(tenantId, userId, "SUPPLIER", s.getId(), "CREATE", null, s, ipAddress);
        return mapSupplier(s);
    }

    @Transactional(readOnly = true)
    public Page<SupplierDto> getSuppliers(String tenantId, Pageable pageable) {
        return supplierRepo.findByTenantId(tenantId, pageable).map(this::mapSupplier);
    }

    @Transactional(readOnly = true)
    public SupplierDto getSupplier(String tenantId, String id) {
        Supplier s = supplierRepo.findByTenantIdAndId(tenantId, id)
                .orElseThrow(() -> new MedShopException.NotFoundException("Supplier not found"));
        return mapSupplier(s);
    }

    @Transactional
    public SupplierDto updateSupplier(String tenantId, String userId, String id, UpdateSupplierRequest req, String ipAddress) {
        Supplier s = supplierRepo.findByTenantIdAndId(tenantId, id)
                .orElseThrow(() -> new MedShopException.NotFoundException("Supplier not found"));

        if (!ValidationPatterns.isValidPhone(req.phone())) {
            throw new MedShopException.BadRequestException("Invalid phone number format: " + req.phone());
        }
        if (req.gstin() != null && !req.gstin().isBlank() && !ValidationPatterns.isValidGstin(req.gstin().trim().toUpperCase())) {
            throw new MedShopException.BadRequestException("Invalid GSTIN format: " + req.gstin());
        }

        s.setName(req.name().trim());
        s.setContactPerson(req.contactPerson());
        s.setPhone(req.phone().trim());
        s.setGstin(req.gstin() != null ? req.gstin().trim().toUpperCase() : null);
        s.setAddress(req.address());
        if (req.active() != null) s.setActive(req.active());

        s = supplierRepo.save(s);
        auditService.record(tenantId, userId, "SUPPLIER", id, "UPDATE", null, s, ipAddress);
        return mapSupplier(s);
    }

    @Transactional
    public void deleteSupplier(String tenantId, String userId, String id, String ipAddress) {
        Supplier s = supplierRepo.findByTenantIdAndId(tenantId, id)
                .orElseThrow(() -> new MedShopException.NotFoundException("Supplier not found"));

        if (medicineRepo.existsByTenantIdAndSupplierId(tenantId, id)) {
            throw new MedShopException.ConflictException(
                    "Supplier is referenced by medicines. Cannot delete; set active=false instead.");
        }

        supplierRepo.delete(s);
        auditService.record(tenantId, userId, "SUPPLIER", id, "DELETE", s, null, ipAddress);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // MEDICINES
    // ══════════════════════════════════════════════════════════════════════════

    @Transactional
    public MedicineFullDto createMedicine(String tenantId, String userId, CreateMedicineRequest req, String ipAddress) {
        validateMedicineRates(req.gstRate().doubleValue(), req.mrp(), req.purchasePrice());

        if (req.categoryId() != null) {
            categoryRepo.findByTenantIdAndId(tenantId, req.categoryId())
                    .orElseThrow(() -> new MedShopException.NotFoundException("Category not found"));
        }
        if (req.supplierId() != null) {
            supplierRepo.findByTenantIdAndId(tenantId, req.supplierId())
                    .orElseThrow(() -> new MedShopException.NotFoundException("Supplier not found"));
        }

        Medicine m = new Medicine();
        m.setTenantId(tenantId);
        m.setName(req.name().trim());
        m.setGenericName(req.genericName() != null ? req.genericName().trim() : null);
        m.setHsnCode(req.hsnCode().trim());
        m.setCategoryId(req.categoryId());
        m.setSupplierId(req.supplierId());
        m.setManufacturer(req.manufacturer());
        m.setForm(req.form());
        m.setStrength(req.strength());
        m.setMrp(req.mrp());
        m.setPurchasePrice(req.purchasePrice());
        m.setGstRate(req.gstRate());
        m.setScheduleH(Boolean.TRUE.equals(req.scheduleH()));
        m.setActive(true);
        if (req.reorderLevel() != null && req.reorderLevel() >= 0) {
            m.setReorderLevel(req.reorderLevel());
        }

        m = medicineRepo.save(m);
        auditService.record(tenantId, userId, "MEDICINE", m.getId(), "CREATE", null, m, ipAddress);

        return mapMedicineFull(m);
    }

    @Transactional(readOnly = true)
    public Page<?> getMedicines(
            String tenantId, String q, String categoryId, String supplierId,
            String form, Boolean scheduleH, Boolean active, Pageable pageable, boolean isCashier) {

        Page<Medicine> page = medicineRepo.findFiltered(tenantId, q, categoryId, supplierId, form, scheduleH, active, pageable);
        if (isCashier) {
            return page.map(this::mapMedicineCashier);
        }
        return page.map(this::mapMedicineFull);
    }

    @Transactional(readOnly = true)
    public Object getMedicineById(String tenantId, String id, boolean isCashier) {
        Medicine m = medicineRepo.findByTenantIdAndId(tenantId, id)
                .orElseThrow(() -> new MedShopException.NotFoundException("Medicine not found"));
        if (isCashier) {
            return mapMedicineCashier(m);
        }
        return mapMedicineFull(m);
    }

    @Transactional
    public MedicineFullDto updateMedicine(String tenantId, String userId, String id, UpdateMedicineRequest req, String ipAddress) {
        Medicine m = medicineRepo.findByTenantIdAndId(tenantId, id)
                .orElseThrow(() -> new MedShopException.NotFoundException("Medicine not found"));

        validateMedicineRates(req.gstRate().doubleValue(), req.mrp(), req.purchasePrice());

        if (req.categoryId() != null) {
            categoryRepo.findByTenantIdAndId(tenantId, req.categoryId())
                    .orElseThrow(() -> new MedShopException.NotFoundException("Category not found"));
        }
        if (req.supplierId() != null) {
            supplierRepo.findByTenantIdAndId(tenantId, req.supplierId())
                    .orElseThrow(() -> new MedShopException.NotFoundException("Supplier not found"));
        }

        m.setName(req.name().trim());
        m.setGenericName(req.genericName() != null ? req.genericName().trim() : null);
        m.setHsnCode(req.hsnCode().trim());
        m.setCategoryId(req.categoryId());
        m.setSupplierId(req.supplierId());
        m.setManufacturer(req.manufacturer());
        m.setForm(req.form());
        m.setStrength(req.strength());
        m.setMrp(req.mrp());
        m.setPurchasePrice(req.purchasePrice());
        m.setGstRate(req.gstRate());
        if (req.scheduleH() != null) m.setScheduleH(req.scheduleH());
        if (req.active() != null) m.setActive(req.active());
        if (req.reorderLevel() != null && req.reorderLevel() >= 0) m.setReorderLevel(req.reorderLevel());

        m = medicineRepo.save(m);
        auditService.record(tenantId, userId, "MEDICINE", id, "UPDATE", null, m, ipAddress);

        return mapMedicineFull(m);
    }

    @Transactional
    public void deleteMedicine(String tenantId, String userId, String id, String ipAddress) {
        Medicine m = medicineRepo.findByTenantIdAndId(tenantId, id)
                .orElseThrow(() -> new MedShopException.NotFoundException("Medicine not found"));

        boolean hasBatches = batchRepo.existsByTenantIdAndMedicineId(tenantId, id);
        if (hasBatches) {
            // Soft delete: sets is_active = false
            m.setActive(false);
            medicineRepo.save(m);
            auditService.record(tenantId, userId, "MEDICINE", id, "DEACTIVATE", null, m, ipAddress);
        } else {
            // Hard delete only if never used
            medicineRepo.delete(m);
            auditService.record(tenantId, userId, "MEDICINE", id, "DELETE", m, null, ipAddress);
        }
    }

    @Transactional(readOnly = true)
    public List<MedicineFullDto> getLowStockMedicines(String tenantId) {
        LocalDate today = LocalDate.now(zoneId);
        List<Medicine> medicines = medicineRepo.findLowStockMedicines(tenantId, today);
        return medicines.stream().map(this::mapMedicineFull).toList();
    }

    private void validateMedicineRates(double gstRate, java.math.BigDecimal mrp, java.math.BigDecimal purchasePrice) {
        if (!allowedGstRates.contains(gstRate)) {
            throw new MedShopException.BadRequestException("GST rate " + gstRate + "% is not an allowed GST rate: " + allowedGstRates);
        }
        if (mrp.compareTo(purchasePrice) < 0) {
            log.warn("Medicine validation warning: MRP ({}) is lower than purchase price ({})", mrp, purchasePrice);
        }
    }

    private CategoryDto mapCategory(Category c, List<CategoryDto> children) {
        return new CategoryDto(c.getId(), c.getName(), c.getDescription(), c.getParentId(), children);
    }

    private SupplierDto mapSupplier(Supplier s) {
        return new SupplierDto(s.getId(), s.getName(), s.getContactPerson(), s.getPhone(), s.getGstin(), s.getAddress(), s.isActive());
    }

    public MedicineFullDto mapMedicineFull(Medicine m) {
        LocalDate today = LocalDate.now(zoneId);
        int available = medicineRepo.getAvailableStock(m.getTenantId(), m.getId(), today);
        LocalDate nearestExpiry = medicineRepo.getNearestExpiry(m.getTenantId(), m.getId(), today);

        String catName = m.getCategoryId() != null ? categoryRepo.findByTenantIdAndId(m.getTenantId(), m.getCategoryId())
                .map(Category::getName).orElse(null) : null;
        String supName = m.getSupplierId() != null ? supplierRepo.findByTenantIdAndId(m.getTenantId(), m.getSupplierId())
                .map(Supplier::getName).orElse(null) : null;

        return new MedicineFullDto(
                m.getId(), m.getName(), m.getGenericName(), m.getHsnCode(),
                m.getCategoryId(), catName, m.getSupplierId(), supName,
                m.getManufacturer(), m.getForm(), m.getStrength(),
                m.getMrp(), m.getPurchasePrice(), m.getGstRate(),
                m.isScheduleH(), m.isActive(), m.getReorderLevel(),
                available, nearestExpiry
        );
    }

    public MedicineCashierDto mapMedicineCashier(Medicine m) {
        LocalDate today = LocalDate.now(zoneId);
        int available = medicineRepo.getAvailableStock(m.getTenantId(), m.getId(), today);
        LocalDate nearestExpiry = medicineRepo.getNearestExpiry(m.getTenantId(), m.getId(), today);

        String catName = m.getCategoryId() != null ? categoryRepo.findByTenantIdAndId(m.getTenantId(), m.getCategoryId())
                .map(Category::getName).orElse(null) : null;

        return new MedicineCashierDto(
                m.getId(), m.getName(), m.getGenericName(), m.getHsnCode(),
                m.getCategoryId(), catName, m.getManufacturer(), m.getForm(), m.getStrength(),
                m.getMrp(), m.getGstRate(), m.isScheduleH(), m.isActive(),
                available, nearestExpiry
        );
    }
}

