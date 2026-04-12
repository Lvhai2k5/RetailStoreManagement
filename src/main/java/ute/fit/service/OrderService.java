package ute.fit.service;

import ute.fit.dto.OrderDTO;
import ute.fit.dto.OrderItemDTO;
import ute.fit.entity.OrdersEntity;
import ute.fit.entity.OrderDetailsEntity;
import ute.fit.entity.ProductsEntity;
import ute.fit.model.OrderStatus;
import ute.fit.repository.OrderDetailRepository;
import ute.fit.repository.OrderRepository;
import ute.fit.repository.ProductRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
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

    // ================= CREATE =================
    @Transactional
    public OrdersEntity createOrder(OrderDTO dto) {

        try {
            // 🔥 1. CREATE ORDER (SQL)
            Integer orderId = orderJdbcRepo.createOrder(false);

            BigDecimal total = BigDecimal.ZERO;

            // 🔥 2. ADD ITEMS (SQL → tự tạo Allocation + Inventory)
            for (OrderItemDTO item : dto.getItems()) {

                ProductsEntity product = productRepo
                        .findById(item.getProductID())
                        .orElseThrow(() -> new RuntimeException("Product not found"));

                BigDecimal price = product.getDefaultSellingPrice();

                total = total.add(
                        price.multiply(BigDecimal.valueOf(item.getQuantity()))
                );

                // 👉 CALL PROC_AddOrderItem
                orderJdbcRepo.addOrderItem(
                        orderId,
                        item.getProductID(),
                        item.getQuantity()
                );
            }

            // 🔥 3. UPDATE TOTAL
            orderJdbcRepo.updateTotal(orderId, total);

            // 🔥 4. RETURN ENTITY
            return orderRepo.findById(orderId)
                    .orElseThrow(() -> new RuntimeException("Order not found"));

        } catch (Exception e) {
            throw new RuntimeException("Create order failed: " + e.getMessage());
        }
    }

    // ================= LIST =================
    public List<OrdersEntity> getAllOrders() {
        return orderRepo.findAll(); // hoặc filter nếu cần
    }

    // ================= DELETE (SOFT) =================
    @Transactional
    public void deleteOrder(Integer id) {

        OrdersEntity order = orderRepo.findById(id).orElseThrow();

        order.setStatus(OrderStatus.Cancelled); // chuẩn DB
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
}