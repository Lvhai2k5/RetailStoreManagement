package ute.fit.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ute.fit.entity.ImportBatchesEntity;
import ute.fit.entity.InventoryTransactionsEntity;
import ute.fit.entity.ProductPriceHistoryEntity;
import ute.fit.entity.ProductTypesMarkupEntity;
import ute.fit.entity.ProductsEntity;
import ute.fit.entity.SuppliersEntity;
import ute.fit.model.ProductStatus;
import ute.fit.model.TransactionType;
import ute.fit.repository.ImportBatchRepository;
import ute.fit.repository.InventoryTransactionRepository;
import ute.fit.repository.ProductPriceHistoryRepository;
import ute.fit.repository.ProductRepository;
import ute.fit.repository.ProductTypeRepository;
import ute.fit.repository.SupplierRepository;
import ute.fit.service.IInventoryService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class InventoryServiceImpl implements IInventoryService {

    @Autowired private ImportBatchRepository batchRepo;
    @Autowired private InventoryTransactionRepository transRepo;
    @Autowired private ProductRepository productRepo;
    @Autowired private SupplierRepository supplierRepo;
    @Autowired private ProductTypeRepository productTypeRepo;
    
    // Thêm Repository Lịch sử giá (Bạn nhớ tạo class này nếu chưa có nhé)
    @Autowired private ProductPriceHistoryRepository priceHistoryRepo;

    @Override
    public Map<String, Object> getInventoryStats() {
        Map<String, Object> stats = new HashMap<>();
        LocalDateTime now = LocalDateTime.now();
        
        stats.put("totalBatches", batchRepo.count());
        stats.put("expiredCount", batchRepo.countExpired(now));
        stats.put("expiringSoonCount", batchRepo.countExpiringSoon(now, now.plusDays(7)));
        
        long lowStockCount = 0;
        List<Map<String, Object>> allBatches = batchRepo.findAllBatchesForUI(Pageable.unpaged()).getContent();
        for (Map<String, Object> b : allBatches) {
            Integer batchId = (Integer) b.get("batchID");
            Integer stock = transRepo.sumQuantityChangeByBatchId(batchId);
            if (stock != null && stock > 0 && stock <= 10) {
                lowStockCount++;
            }
        }
        stats.put("lowStockCount", lowStockCount);
        
        Double totalValue = batchRepo.sumTotalInventoryValue();
        stats.put("totalValue", totalValue != null ? totalValue : 0.0);
        
        return stats;
    }

    @Override
    public Page<Map<String, Object>> getInventoryList(String keyword, String statusFilter, Integer productTypeId, Pageable pageable) {
        List<Map<String, Object>> allBatches = batchRepo.findAllBatchesForUI(Pageable.unpaged()).getContent();
        List<Map<String, Object>> filteredList = new ArrayList<>();

        for (Map<String, Object> batch : allBatches) {
            Map<String, Object> row = new HashMap<>(batch);
            Integer batchId = (Integer) batch.get("batchID");

            if (productTypeId != null) {
                Integer typeId = (Integer) batch.get("productTypeID"); 
                if (!productTypeId.equals(typeId)) continue;
            }

            if (keyword != null && !keyword.trim().isEmpty()) {
                String searchStr = keyword.toLowerCase();
                String productName = (String) batch.get("productName");
                if (!String.valueOf(batchId).contains(searchStr) && 
                    (productName == null || !productName.toLowerCase().contains(searchStr))) {
                    continue; 
                }
            }

            Integer stockFromTrans = transRepo.sumQuantityChangeByBatchId(batchId);
            int currentStock = (stockFromTrans != null) ? stockFromTrans : 0;
            row.put("availableQty", currentStock);

            LocalDateTime expiry = (LocalDateTime) batch.get("expiryDate");
            String status = "AVAILABLE"; 
            
            if (expiry != null && expiry.isBefore(LocalDateTime.now())) {
                status = "EXPIRED"; 
            } else if (expiry != null && expiry.isBefore(LocalDateTime.now().plusDays(7))) {
                status = "NEAR_EXPIRY"; 
            } else if (currentStock <= 0) {
                status = "OUT_OF_STOCK"; 
            } else if (currentStock <= 10) {
                status = "LOW_STOCK"; 
            }
            row.put("status", status);

            if (statusFilter != null && !statusFilter.isEmpty() && !statusFilter.equals(status)) {
                continue; 
            }

            filteredList.add(row);
        }

        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), filteredList.size());
        List<Map<String, Object>> pageContent = (start <= end) ? filteredList.subList(start, end) : new ArrayList<>();
        
        return new PageImpl<>(pageContent, pageable, filteredList.size());
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getBatchTraceDetails(Integer batchId) {
        Map<String, Object> result = new HashMap<>();
        ImportBatchesEntity batch = batchRepo.findById(batchId).orElseThrow();
            
        result.put("batchID", batch.getBatchID());
        result.put("productName", batch.getProduct().getName());
        result.put("supplierName", batch.getSupplier().getName());
        result.put("importDate", batch.getImportDate());
        result.put("importQty", batch.getQuantity());
        
        List<InventoryTransactionsEntity> trans = transRepo.findByBatchId(batchId);
        List<Map<String, Object>> history = new ArrayList<>();
        for (InventoryTransactionsEntity t : trans) {
            Map<String, Object> map = new HashMap<>();
            map.put("date", t.getTransactionDate());
            map.put("type", t.getTransactionType().name());
            map.put("change", t.getQuantityChange());
            map.put("ref", t.getReferenceID());
            history.add(map);
        }
        result.put("history", history);
        return result;
    }

    @Override
    public List<Map<String, Object>> getProductOptions() {
        return productRepo.findAll().stream().map(p -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", p.getProductID());
            map.put("name", p.getName());
            
            if (p.getProductType() != null) {
                map.put("markup", p.getProductType().getMarkupPercent());
            } else {
                map.put("markup", 0); 
            }
            return map;
        }).collect(Collectors.toList());
    }

    @Override
    public List<Map<String, Object>> getSupplierOptions() {
        return supplierRepo.findAll().stream().map(s -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", s.getSupplierID());
            map.put("name", s.getName());
            return map;
        }).collect(Collectors.toList());
    }

    // =======================================================
    // HÀM NHẬP KHO: ĐÃ TÍCH HỢP LOGIC GIÁ TRUNG BÌNH 2 LÔ
    // =======================================================
    @Override
    @Transactional
    public void importNewBatch(String productId, Integer supplierId, Integer quantity, Double totalImportValue, Double sellingPrice, String expiryDate) {
        ProductsEntity product = productRepo.findById(productId).orElseThrow();
        SuppliersEntity supplier = supplierRepo.findById(supplierId).orElseThrow();

        Integer maxBatchNumber = batchRepo.findMaxBatchNumberByProductId(productId);
        int nextBatchNumber = (maxBatchNumber != null ? maxBatchNumber : 0) + 1;

        Double unitImportPrice = 0.0;
        if (quantity > 0) {
            unitImportPrice = totalImportValue / quantity;
        }

        // 1. LƯU LÔ HÀNG MỚI
        ImportBatchesEntity batch = new ImportBatchesEntity();
        batch.setProduct(product);
        batch.setSupplier(supplier);
        batch.setQuantity(quantity);
        batch.setImportPrice(BigDecimal.valueOf(unitImportPrice)); 
        batch.setSellingPrice(BigDecimal.valueOf(sellingPrice));
        batch.setBatchNumber(nextBatchNumber);
        
        if (expiryDate != null && !expiryDate.trim().isEmpty()) {
            batch.setExpiryDate(LocalDate.parse(expiryDate).atStartOfDay());
        }
        batch = batchRepo.save(batch);

        // 2. GHI NHẬN GIAO DỊCH
        InventoryTransactionsEntity tx = new InventoryTransactionsEntity();
        tx.setBatch(batch);
        tx.setQuantityChange(quantity);
        tx.setTransactionType(TransactionType.IMPORT);
        transRepo.save(tx);

        // 3. TÍNH GIÁ BÁN TRUNG BÌNH CỦA 2 LÔ GẦN NHẤT
        List<ImportBatchesEntity> top2Batches = batchRepo.findTop2ByProduct_ProductIDOrderByBatchIDDesc(productId);
        BigDecimal newDefaultPrice;
        
        if (top2Batches.size() >= 2) {
            BigDecimal price1 = top2Batches.get(0).getSellingPrice();
            BigDecimal price2 = top2Batches.get(1).getSellingPrice();
            newDefaultPrice = price1.add(price2).divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
        } else {
            newDefaultPrice = top2Batches.get(0).getSellingPrice();
        }

        // 4. CẬP NHẬT LẠI GIÁ BÁN VÀO PRODUCT
        product.setDefaultSellingPrice(newDefaultPrice);
        productRepo.save(product);

        // 5. GHI LẠI LỊCH SỬ THAY ĐỔI GIÁ
        ProductPriceHistoryEntity priceHistory = new ProductPriceHistoryEntity();
        priceHistory.setProduct(product);
        priceHistory.setSellingPrice(newDefaultPrice);
        priceHistory.setChangeReason("NEW_BATCH");
        priceHistory.setEffectiveDate(LocalDateTime.now());
        priceHistoryRepo.save(priceHistory);
    }

    // =======================================================
    // HÀM BỊ MẤT ĐÃ ĐƯỢC THÊM LẠI VÀO ĐÂY
    // =======================================================
    @Override
    public List<Map<String, Object>> getProductTypeOptions() {
        return productTypeRepo.findAll().stream().map(pt -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", pt.getProductTypeID());
            map.put("name", pt.getName());
            map.put("markup", pt.getMarkupPercent()); 
            return map;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public Map<String, Object> quickAddProduct(String productId, String name, Integer productTypeId) {
        if (productId == null || productId.length() > 100) {
            throw new IllegalArgumentException("Mã sản phẩm không hợp lệ hoặc vượt quá 100 ký tự.");
        }
        if (productRepo.existsById(productId)) {
            throw new IllegalArgumentException("Mã sản phẩm '" + productId + "' đã tồn tại trong hệ thống!");
        }

        ProductTypesMarkupEntity type = productTypeRepo.findById(productTypeId)
            .orElseThrow(() -> new IllegalArgumentException("Loại sản phẩm không tồn tại!"));
        
        ProductsEntity newProduct = new ProductsEntity();
        newProduct.setProductID(productId.trim());
        newProduct.setName(name.trim());
        newProduct.setProductType(type);
        newProduct.setDefaultSellingPrice(BigDecimal.ZERO);
        newProduct.setStatus(ProductStatus.BUYING);
        
        productRepo.save(newProduct);
        
        Map<String, Object> result = new HashMap<>();
        result.put("id", newProduct.getProductID());
        result.put("name", newProduct.getName());
        return result;
    }
    
    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getBatchForEdit(Integer batchId) {
        ImportBatchesEntity batch = batchRepo.findById(batchId).orElseThrow();
        Map<String, Object> map = new HashMap<>();
        map.put("batchId", batch.getBatchID());
        map.put("productId", batch.getProduct().getProductID());
        map.put("productName", batch.getProduct().getName());
        map.put("supplierId", batch.getSupplier().getSupplierID());
        map.put("quantity", batch.getQuantity()); 
        map.put("importPrice", batch.getImportPrice());
        map.put("sellingPrice", batch.getSellingPrice());
        map.put("expiryDate", batch.getExpiryDate() != null ? batch.getExpiryDate().toLocalDate().toString() : "");
        return map;
    }

    @Override
    @Transactional
    public void updateBatch(Integer batchId, Integer supplierId, Integer quantity, Double importPrice, Double sellingPrice, String expiryDate) {
        ImportBatchesEntity batch = batchRepo.findById(batchId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy lô hàng"));

        Integer currentStock = transRepo.sumQuantityChangeByBatchId(batchId);
        int available = (currentStock != null) ? currentStock : 0;
        int usedQty = batch.getQuantity() - available; 

        if (quantity < usedQty) {
            throw new IllegalArgumentException("Lô này đã bán/xuất " + usedQty + " sản phẩm. Không thể sửa tổng số lượng nhập nhỏ hơn " + usedQty + "!");
        }

        int diff = quantity - batch.getQuantity();
        if (diff != 0) {
            InventoryTransactionsEntity tx = new InventoryTransactionsEntity();
            tx.setBatch(batch);
            tx.setQuantityChange(diff);
            tx.setTransactionType(TransactionType.IMPORT); 
            transRepo.save(tx);
        }

        batch.setSupplier(supplierRepo.findById(supplierId).orElseThrow());
        batch.setQuantity(quantity);
        batch.setImportPrice(BigDecimal.valueOf(importPrice));
        batch.setSellingPrice(BigDecimal.valueOf(sellingPrice));

        if (expiryDate != null && !expiryDate.trim().isEmpty()) {
            batch.setExpiryDate(LocalDate.parse(expiryDate).atStartOfDay());
        } else {
            batch.setExpiryDate(null); 
        }

        batchRepo.save(batch);
    }
}