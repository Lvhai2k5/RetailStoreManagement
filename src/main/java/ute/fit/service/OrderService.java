package ute.fit.service;

import ute.fit.dto.OrderDTO;
import ute.fit.dto.OrderItemDTO;
import ute.fit.entity.OrdersEntity;
import ute.fit.entity.OrderDetailsEntity;
import ute.fit.entity.ProductsEntity;
import ute.fit.repository.OrderRepository;
import ute.fit.repository.ProductRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepo;
    private final ProductRepository productRepo;

    @Transactional
    public void createOrder(OrderDTO dto) {

        // 1. tạo Order
        OrdersEntity order = new OrdersEntity();
        order.setTotalAmount(dto.getTotalAmount());
        order.setCreatedDate(LocalDateTime.now());

        List<OrderDetailsEntity> details = new ArrayList<>();

        // 2. loop từng item
        for (OrderItemDTO item : dto.getItems()) {

            ProductsEntity product = productRepo.findById(item.getProductID())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm"));

            OrderDetailsEntity detail = new OrderDetailsEntity();
            detail.setOrder(order);
            detail.setProduct(product);
            detail.setQuantity(item.getQuantity());

            detail.setUnitPrice(product.getDefaultSellingPrice());

            details.add(detail);
        }

        order.setOrderDetails(details); 

        // 3. save (cascade)
        orderRepo.save(order);
    }
}