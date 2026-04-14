package ute.fit.service;

import ute.fit.dto.OrderDTO;
import ute.fit.dto.OrderItemDTO;
import ute.fit.entity.*;
import ute.fit.model.OrderStatus;
import ute.fit.model.TransactionType;
import ute.fit.repository.*;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepo;
    private final ProductRepository productRepo;
    private final OrderDetailRepository orderDetailRepo;
    private final OrderJdbcRepository orderJdbcRepo;
    private final JdbcTemplate jdbcTemplate;
    private final ImportBatchRepository importBatchRepo;
    private final InventoryTransactionRepository inventoryRepo;
    private final SaleAllocationRepository saleAllocationRepo;

    public void payOrder(Integer orderId, String method) {
        OrdersEntity order = orderRepo.findById(orderId).orElseThrow();

        order.setPaymentMethod(method);
        order.setStatus(OrderStatus.Paid);
        order.setPaidDate(LocalDateTime.now());

        orderRepo.save(order);
    }

@Transactional
public OrdersEntity createOrder(OrderDTO dto) {

    // =========================
    // 1. CREATE ORDER (CHƯA SAVE)
    // =========================
    OrdersEntity order = new OrdersEntity();
    order.setStatus(OrderStatus.Pending);
    order.setCreatedDate(LocalDateTime.now());
    order.setTotalAmount(dto.getTotalAmount());

    List<OrderDetailsEntity> details = new ArrayList<>();

    // =========================
    // 2. BUILD ORDER DETAILS
    // =========================
    for (OrderItemDTO item : dto.getItems()) {

        ProductsEntity product = productRepo.findById(item.getProductID())
                .orElseThrow(() -> new RuntimeException("Product not found"));

        OrderDetailsEntity detail = new OrderDetailsEntity();
        detail.setOrder(order);
        detail.setProduct(product);
        detail.setQuantity(item.getQuantity());
        detail.setUnitPrice(product.getDefaultSellingPrice());

        details.add(detail);
    }

    order.setOrderDetails(details);

    // =====================================
    // 3. SAVE ORDER + DETAILS (IMPORTANT)
    // =====================================
    order = orderRepo.save(order);

    // =====================================
    // 4. FIFO STOCK ALLOCATION
    // =====================================
    for (OrderDetailsEntity detail : order.getOrderDetails()) {

        int remain = detail.getQuantity();

        List<ImportBatchesEntity> batches =
                importBatchRepo.findByProductID(detail.getProduct().getProductID());

        for (ImportBatchesEntity batch : batches) {

            if (remain <= 0) break;

            int stock = inventoryRepo.sumStockByBatch(batch.getBatchID());

            if (stock <= 0) continue;

            int take = Math.min(stock, remain);

            // =========================
            // 5. SALE ALLOCATION
            // =========================
            SaleAllocationsEntity sa = new SaleAllocationsEntity();
            sa.setOrderDetail(detail);   // OK vì detail đã persist
            sa.setBatch(batch);
            sa.setQuantity(take);

            saleAllocationRepo.save(sa);

            // =========================
            // 6. INVENTORY TRANSACTION
            // =========================
            InventoryTransactionsEntity it = new InventoryTransactionsEntity();
            it.setBatch(batch);
            it.setQuantityChange(-take);
            it.setTransactionType(TransactionType.SALE);
            it.setIsSellable(true);
            it.setReferenceID(order.getOrderID()); // OK vì order đã save

            inventoryRepo.save(it);

            remain -= take;
        }

        // =========================
        // 7. CHECK STOCK FAIL
        // =========================
        if (remain > 0) {
            throw new RuntimeException(
                    "Không đủ tồn kho cho sản phẩm: " +
                            detail.getProduct().getName()
            );
        }
    }

    return order;
}


    // ================= LIST =================
    public List<OrdersEntity> getAllOrders() {
        return orderRepo.findAll(); // hoặc filter nếu cần
    }

    // ================= DELETE (SOFT) =================
    @Transactional
    public void deleteOrder(Integer id) {

        OrdersEntity order = orderRepo.findById(id).orElseThrow();

        order.setStatus(OrderStatus.Cancelled);
        orderRepo.save(order);
    }

    // ================= UPDATE DETAIL =================
    @Transactional
    public void updateOrderDetail(Integer detailId, Integer quantity){

        OrderDetailsEntity detail = orderDetailRepo
                .findById(detailId)
                .orElseThrow();

        OrdersEntity order = detail.getOrder();

        // ❗ chỉ cho sửa khi Pending
        if(order.getStatus() != OrderStatus.Pending){
            throw new RuntimeException("Chỉ sửa được đơn Pending");
        }

        // update quantity
        detail.setQuantity(quantity);
        orderDetailRepo.save(detail);

        // 🔥 update lại TOTAL ORDER
        BigDecimal total = order.getOrderDetails()
                .stream()
                .map(d -> d.getUnitPrice()
                        .multiply(BigDecimal.valueOf(d.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        order.setTotalAmount(total);
        orderRepo.save(order);
    }

    // ================= PAYMENT =================
    @Transactional
    public void updatePayment(Integer orderId, String method) {

        OrdersEntity order = orderRepo.findById(orderId).orElseThrow();

        order.setPaymentMethod(method);
        order.setStatus(OrderStatus.Paid);
        order.setPaidDate(LocalDateTime.now());

        orderRepo.save(order);
    }

    public OrdersEntity getById(Integer id) {
        return orderRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng với ID: " + id));
    }

    @Transactional
    public void removeDetail(Integer detailId){

        OrderDetailsEntity detail = orderDetailRepo
                .findById(detailId)
                .orElseThrow();

        OrdersEntity order = detail.getOrder();

        // ❗ chỉ cho xóa khi Pending
        if(order.getStatus() != OrderStatus.Pending){
            throw new RuntimeException("Chỉ xóa được đơn Pending");
        }

        orderDetailRepo.delete(detail);

        // 🔥 cập nhật lại total
        BigDecimal total = order.getOrderDetails()
                .stream()
                .filter(d -> d.getOrderDetailID() != detailId)
                .map(d -> d.getUnitPrice()
                        .multiply(BigDecimal.valueOf(d.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        order.setTotalAmount(total);
        orderRepo.save(order);
    }
    public boolean checkStock(String productID, int qty) {

        String sql = """
        SELECT ISNULL(SUM(it.QuantityChange),0)
        FROM InventoryTransactions it
        JOIN ImportBatches b ON it.BatchID = b.BatchID
        WHERE b.ProductID = ? AND it.IsSellable = 1
    """;

        Integer stock = jdbcTemplate.queryForObject(sql, Integer.class, productID);

        return stock != null && stock >= qty;
    }

}