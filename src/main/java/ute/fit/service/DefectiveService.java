package ute.fit.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ute.fit.dto.DefectiveHandleFormDTO;
import ute.fit.entity.DefectiveProductsEntity;
import ute.fit.entity.ImportBatchesEntity;
import ute.fit.entity.InventoryTransactionsEntity;
import ute.fit.model.TransactionType;
import ute.fit.repository.DefectiveProductsRepository;
import ute.fit.repository.DefectiveQueryRepository;
import ute.fit.repository.ImportBatchRepository;
import ute.fit.repository.InventoryTransactionsRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DefectiveService {

    private final DefectiveQueryRepository defectiveQueryRepository;
    private final ImportBatchRepository importBatchRepository;
    private final InventoryTransactionsRepository inventoryTransactionsRepository;
    private final DefectiveProductsRepository defectiveProductsRepository;

    public DefectiveService(DefectiveQueryRepository defectiveQueryRepository,
                            ImportBatchRepository importBatchRepository,
                            InventoryTransactionsRepository inventoryTransactionsRepository,
                            DefectiveProductsRepository defectiveProductsRepository) {
        this.defectiveQueryRepository = defectiveQueryRepository;
        this.importBatchRepository = importBatchRepository;
        this.inventoryTransactionsRepository = inventoryTransactionsRepository;
        this.defectiveProductsRepository = defectiveProductsRepository;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getManagementViewData(LocalDate fromDate,
                                                     LocalDate toDate,
                                                     String expiryStatus,
                                                     String reason,
                                                     String keyword) {
        Map<String, Object> stats = defectiveQueryRepository.getDashboardStats();
        List<Map<String, Object>> rows = defectiveQueryRepository.searchBatchDefectiveData(
                fromDate, toDate, expiryStatus, reason, keyword
        );

        Map<String, Object> model = new HashMap<>();
        model.put("rows", rows);
        model.put("totalBatches", safeLong(stats.get("totalBatches")));
        model.put("expiringSoonBatches", safeLong(stats.get("expiringSoonBatches")));
        model.put("expiredBatches", safeLong(stats.get("expiredBatches")));
        model.put("totalDefectiveQuantity", safeLong(stats.get("totalDefectiveQuantity")));
        model.put("fromDate", fromDate);
        model.put("toDate", toDate);
        model.put("expiryStatus", expiryStatus == null ? "ALL" : expiryStatus);
        model.put("reason", reason == null ? "ALL" : reason);
        model.put("keyword", keyword == null ? "" : keyword);
        model.put("reasonOptions", buildReasonOptions());
        return model;
    }

    @Transactional(readOnly = true)
    public DefectiveHandleFormDTO initHandleForm(Integer batchId) {
        DefectiveHandleFormDTO dto = new DefectiveHandleFormDTO();
        dto.setBatchID(batchId);
        dto.setQuantity(1);
        dto.setReason("HET_HAN");
        return dto;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getBatchInfoForHandle(Integer batchId) {
        if (batchId == null) {
            return null;
        }
        return defectiveQueryRepository.getBatchHandleInfo(batchId);
    }

    @Transactional
    public void handleDefective(DefectiveHandleFormDTO form) {
        validateBasicForm(form);
        String reason = normalizeReason(form.getReason(), form.getNote());
        handleDefectiveForBatch(form.getBatchID(), form.getQuantity(), reason);
    }

    @Transactional
    public int processAllExpiredBatches() {
        List<Integer> expiredBatchIds = importBatchRepository.findExpiredBatchIds(LocalDateTime.now());
        int handled = 0;

        for (Integer batchId : expiredBatchIds) {
            try {
                ImportBatchesEntity batch = importBatchRepository.findByIdForUpdate(batchId).orElse(null);
                if (batch == null) {
                    continue;
                }
                int sellableStock = getSellableStock(batchId);
                if (sellableStock > 0) {
                    persistDefectiveAndTransaction(batch, sellableStock, "HET_HAN");
                    handled++;
                }
            } catch (Exception ignored) {
                // Continue with next batch if one batch fails.
            }
        }

        return handled;
    }

    private void validateBasicForm(DefectiveHandleFormDTO form) {
        if (form.getBatchID() == null) {
            throw new IllegalArgumentException("Ban chua chon lo hang.");
        }
        if (form.getQuantity() == null || form.getQuantity() <= 0) {
            throw new IllegalArgumentException("So luong loi phai lon hon 0.");
        }
        if (form.getReason() == null || form.getReason().trim().isEmpty()) {
            throw new IllegalArgumentException("Ban can chon ly do xu ly.");
        }
    }

    private void handleDefectiveForBatch(Integer batchId, Integer quantity, String reason) {
        ImportBatchesEntity batch = importBatchRepository.findByIdForUpdate(batchId)
                .orElseThrow(() -> new IllegalArgumentException("BatchID khong ton tai."));

        int sellableStock = getSellableStock(batchId);
        if (sellableStock <= 0) {
            throw new IllegalArgumentException("Lo nay khong con ton ban duoc de xu ly.");
        }
        if (quantity > sellableStock) {
            throw new IllegalArgumentException(
                    "So luong loi (" + quantity + ") vuot qua ton ban duoc hien tai (" + sellableStock + ")."
            );
        }

        persistDefectiveAndTransaction(batch, quantity, reason);
    }

    private void persistDefectiveAndTransaction(ImportBatchesEntity batch, Integer quantity, String reason) {
        DefectiveProductsEntity defective = DefectiveProductsEntity.builder()
                .batch(batch)
                .quantity(quantity)
                .reason(reason)
                .importPrice(batch.getImportPrice())
                .createdDate(LocalDateTime.now())
                .build();

        DefectiveProductsEntity savedDefective = defectiveProductsRepository.save(defective);

        InventoryTransactionsEntity tx = InventoryTransactionsEntity.builder()
                .batch(batch)
                .quantityChange(-quantity)
                .transactionType(TransactionType.DEFECT)
                .isSellable(true)
                .referenceID(savedDefective.getDefectiveID())
                .transactionDate(LocalDateTime.now())
                .build();

        inventoryTransactionsRepository.save(tx);
    }

    private String normalizeReason(String reason, String note) {
        String base = reason.trim().toUpperCase();
        if (note == null || note.trim().isEmpty()) {
            return base;
        }
        return base + " | " + note.trim();
    }

    private int getSellableStock(Integer batchId) {
        Integer stock = inventoryTransactionsRepository.getSellableStockByBatchId(batchId);
        return stock == null ? 0 : stock;
    }

    private long safeLong(Object value) {
        if (value == null) {
            return 0L;
        }
        if (value instanceof Number n) {
            return n.longValue();
        }
        return 0L;
    }

    private List<String> buildReasonOptions() {
        List<String> defaults = List.of("HET_HAN", "VO_BAO_BI", "MEO_MOC", "KHAC");
        List<String> dbReasons = defectiveQueryRepository.findDistinctReasons();
        List<String> merged = new ArrayList<>(defaults);

        for (String r : dbReasons) {
            if (r == null) {
                continue;
            }
            String trim = r.trim();
            if (!trim.isEmpty() && !merged.contains(trim)) {
                merged.add(trim);
            }
        }
        return merged;
    }
}
