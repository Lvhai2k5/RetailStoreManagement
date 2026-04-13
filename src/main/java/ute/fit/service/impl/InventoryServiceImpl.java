package ute.fit.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ute.fit.entity.ImportBatchesEntity;
import ute.fit.entity.InventoryTransactionsEntity;
import ute.fit.entity.ProductTypesMarkupEntity;
import ute.fit.entity.ProductsEntity;
import ute.fit.entity.SuppliersEntity;
import ute.fit.model.ProductStatus;
import ute.fit.model.TransactionType;
import ute.fit.repository.ImportBatchRepository;
import ute.fit.repository.InventoryTransactionRepository;
import ute.fit.repository.ProductRepository;
import ute.fit.repository.ProductTypeRepository;
import ute.fit.repository.SupplierRepository;
import ute.fit.service.IInventoryService;

import java.math.BigDecimal;
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
    @Autowired private ProductTypeRepository productTypeRepo; // Thêm repo này để xử lý quick add

    @Override
    public Map<String, Object> getInventoryStats() {
        Map<String, Object> stats = new HashMap<>();
        LocalDateTime now = LocalDateTime.now();
        
        stats.put("totalBatches", batchRepo.count());
        stats.put("expiredCount", batchRepo.countExpired(now));
        stats.put("expiringSoonCount", batchRepo.countExpiringSoon(now, now.plusDays(7)));
        
        // Đếm số lô Sắp hết hàng (Tồn kho > 0 và <= 10)
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
    public Page<Map<String, Object>> getInventoryList(String keyword, String statusFilter, Pageable pageable) {
        List<Map<String, Object>> allBatches = batchRepo.findAllBatchesForUI(Pageable.unpaged()).getContent();
        List<Map<String, Object>> filteredList = new ArrayList<>();

        for (Map<String, Object> batch : allBatches) {
            Map<String, Object> row = new HashMap<>(batch);
            Integer batchId = (Integer) batch.get("batchID");
            String productName = (String) batch.get("productName");

            // 1. Lọc Keyword
            if (keyword != null && !keyword.trim().isEmpty()) {
                String searchStr = keyword.toLowerCase();
                if (!String.valueOf(batchId).contains(searchStr) && 
                    (productName == null || !productName.toLowerCase().contains(searchStr))) {
                    continue; 
                }
            }

            // 2. Tính tồn kho động
            Integer stockFromTrans = transRepo.sumQuantityChangeByBatchId(batchId);
            int currentStock = (stockFromTrans != null) ? stockFromTrans : 0;
            row.put("availableQty", currentStock);

            // 3. Phân loại trạng thái: Ưu tiên Hạn dùng (Đỏ) -> Hết hàng (Xám) -> Sắp hết hàng (Vàng)
            LocalDateTime expiry = (LocalDateTime) batch.get("expiryDate");
            String status = "AVAILABLE"; 
            
            if (expiry != null && expiry.isBefore(LocalDateTime.now())) {
                status = "EXPIRED"; // Đã hết hạn (Đỏ đậm)
            } else if (expiry != null && expiry.isBefore(LocalDateTime.now().plusDays(7))) {
                status = "NEAR_EXPIRY"; // Sắp hết hạn (Đỏ nhạt)
            } else if (currentStock <= 0) {
                status = "OUT_OF_STOCK"; // Hết hàng (Xám)
            } else if (currentStock <= 10) {
                status = "LOW_STOCK"; // Sắp hết hàng (Vàng)
            }
            row.put("status", status);

            // 4. Lọc Trạng thái
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

    @Override
    @Transactional
    public void importNewBatch(String productId, Integer supplierId, Integer quantity, Double totalImportValue, Double sellingPrice, String expiryDate) {
        ProductsEntity product = productRepo.findById(productId).orElseThrow();
        SuppliersEntity supplier = supplierRepo.findById(supplierId).orElseThrow();

        Integer maxBatchNumber = batchRepo.findMaxBatchNumberByProductId(productId);
        int nextBatchNumber = (maxBatchNumber != null ? maxBatchNumber : 0) + 1;

        // TÍNH GIÁ NHẬP BÌNH QUÂN (Giá vốn 1 sản phẩm)
        Double unitImportPrice = 0.0;
        if (quantity > 0) {
            unitImportPrice = totalImportValue / quantity;
        }

        ImportBatchesEntity batch = new ImportBatchesEntity();
        batch.setProduct(product);
        batch.setSupplier(supplier);
        batch.setQuantity(quantity);
        batch.setImportPrice(BigDecimal.valueOf(unitImportPrice)); // Lưu giá bình quân vào DB
        batch.setSellingPrice(BigDecimal.valueOf(sellingPrice));
        batch.setBatchNumber(nextBatchNumber);
        
        if (expiryDate != null && !expiryDate.trim().isEmpty()) {
            batch.setExpiryDate(LocalDate.parse(expiryDate).atStartOfDay());
        }
        batch = batchRepo.save(batch);

        InventoryTransactionsEntity tx = new InventoryTransactionsEntity();
        tx.setBatch(batch);
        tx.setQuantityChange(quantity);
        tx.setTransactionType(TransactionType.IMPORT);
        transRepo.save(tx);
    }

 // 1. Thêm hàm lấy danh sách Loại sản phẩm
    @Override
    public List<Map<String, Object>> getProductTypeOptions() {
        return productTypeRepo.findAll().stream().map(pt -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", pt.getProductTypeID());
            map.put("name", pt.getName());
            return map;
        }).collect(Collectors.toList());
    }

    // 2. Cập nhật hàm quickAddProduct để bắt lỗi Khóa Chính (Trùng ID)
    @Override
    @Transactional
    public Map<String, Object> quickAddProduct(String productId, String name, Integer productTypeId) {
        // KIỂM TRA RÀNG BUỘC: Mã SP không được quá 100 ký tự (theo DDL)
        if (productId == null || productId.length() > 100) {
            throw new IllegalArgumentException("Mã sản phẩm không hợp lệ hoặc vượt quá 100 ký tự.");
        }

        // KIỂM TRA KHÓA CHÍNH: Mã SP đã tồn tại chưa?
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
        map.put("quantity", batch.getQuantity()); // Số lượng nhập ban đầu
        map.put("importPrice", batch.getImportPrice());
        map.put("sellingPrice", batch.getSellingPrice());
        // Lấy hạn sử dụng cũ ném ra UI
        map.put("expiryDate", batch.getExpiryDate() != null ? batch.getExpiryDate().toLocalDate().toString() : "");
        return map;
    }

    @Override
    @Transactional
    public void updateBatch(Integer batchId, Integer supplierId, Integer quantity, Double importPrice, Double sellingPrice, String expiryDate) {
        ImportBatchesEntity batch = batchRepo.findById(batchId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy lô hàng"));

        // 1. RÀNG BUỘC SỐ LƯỢNG: Tính xem lô này đã bị bán/xuất bao nhiêu cái
        Integer currentStock = transRepo.sumQuantityChangeByBatchId(batchId);
        int available = (currentStock != null) ? currentStock : 0;
        int usedQty = batch.getQuantity() - available; // Số lượng đã xuất ra khỏi kho

        // Nếu sửa tổng số lượng NHỎ HƠN số lượng đã bán -> Chặn lại
        if (quantity < usedQty) {
            throw new IllegalArgumentException("Lô này đã bán/xuất " + usedQty + " sản phẩm. Không thể sửa tổng số lượng nhập nhỏ hơn " + usedQty + "!");
        }

        // 2. GHI NHẬN LỊCH SỬ CHÊNH LỆCH: Nếu số lượng bị thay đổi, tự động bù trừ
        int diff = quantity - batch.getQuantity();
        if (diff != 0) {
            InventoryTransactionsEntity tx = new InventoryTransactionsEntity();
            tx.setBatch(batch);
            tx.setQuantityChange(diff);
            // Ghi nhận là IMPORT (nhập bổ sung / rút bớt) để khớp số
            tx.setTransactionType(TransactionType.IMPORT); 
            transRepo.save(tx);
        }

        // 3. CẬP NHẬT CÁC THÔNG TIN ĐƯỢC PHÉP SỬA (Tuyệt đối không có lệnh update ProductID)
        batch.setSupplier(supplierRepo.findById(supplierId).orElseThrow());
        batch.setQuantity(quantity);
        batch.setImportPrice(BigDecimal.valueOf(importPrice));
        batch.setSellingPrice(BigDecimal.valueOf(sellingPrice));

        // 4. CẬP NHẬT HẠN SỬ DỤNG
        if (expiryDate != null && !expiryDate.trim().isEmpty()) {
            batch.setExpiryDate(LocalDate.parse(expiryDate).atStartOfDay());
        } else {
            batch.setExpiryDate(null); // Trường hợp ngta xóa hạn dùng đi
        }

        batchRepo.save(batch);
    }
}