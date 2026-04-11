package ute.fit.service.impl;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import ute.fit.dto.returns.ReturnCreateDTO;
import ute.fit.dto.returns.ReturnDTO;
import ute.fit.dto.returns.ReturnOrderFormDTO;
import ute.fit.dto.returns.ReturnOrderItemDTO;
import ute.fit.dto.returns.ReturnOrderSummaryDTO;
import ute.fit.entity.InventoryTransactionsEntity;
import ute.fit.entity.OrderDetailsEntity;
import ute.fit.entity.OrdersEntity;
import ute.fit.entity.ReturnDetailsEntity;
import ute.fit.entity.ReturnsEntity;
import ute.fit.entity.SaleAllocationsEntity;
import ute.fit.model.OrderStatus;
import ute.fit.model.TransactionType;
import ute.fit.repository.InventoryTransactionRepository;
import ute.fit.repository.OrderDetailRepository;
import ute.fit.repository.OrderRepository;
import ute.fit.repository.ReturnDetailRepository;
import ute.fit.repository.ReturnRepository;
import ute.fit.repository.SaleAllocationRepository;
import ute.fit.service.IReturnService;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReturnServiceImpl implements IReturnService {

    private final ReturnRepository returnRepository;
    private final ReturnDetailRepository returnDetailRepository;
    private final InventoryTransactionRepository inventoryTransactionRepository;
    private final OrderRepository orderRepository;
    private final OrderDetailRepository orderDetailRepository;
    private final SaleAllocationRepository saleAllocationRepository;

    @Override
    public Page<ReturnDTO> getAllReturns(Pageable pageable) {
        return returnRepository.findAll(pageable)
                .map(this::mapToDto);
    }

    @Override
    public ReturnDTO getReturnById(Integer returnId) {
        return returnRepository.findById(returnId)
                .map(this::mapToDto)
                .orElse(null);
    }

    @Override
    public List<ReturnOrderSummaryDTO> getPaidOrders() {
        return orderRepository.findByStatus(OrderStatus.Paid).stream()
                .map(order -> ReturnOrderSummaryDTO.builder()
                        .orderId(order.getOrderID())
                        .orderCode("DH-" + order.getOrderID())
                        .orderStatus(order.getStatus() != null ? order.getStatus().name() : null)
                        .paymentMethod(order.getPaymentMethod())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public ReturnOrderFormDTO getReturnOrderForm(Integer orderId) {
        OrdersEntity order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));

        List<ReturnOrderItemDTO> items = order.getOrderDetails().stream()
                .map(orderDetail -> {
                    int returnedQuantity = returnDetailRepository.getReturnedQuantityByOrderDetailId(orderDetail.getOrderDetailID());
                    int availableQuantity = Math.max(0, orderDetail.getQuantity() - returnedQuantity);
                    return ReturnOrderItemDTO.builder()
                            .orderDetailId(orderDetail.getOrderDetailID())
                            .productName(orderDetail.getProduct() != null ? orderDetail.getProduct().getName() : "")
                            .quantity(orderDetail.getQuantity())
                            .returnedQuantity(returnedQuantity)
                            .availableQuantity(availableQuantity)
                            .unitPrice(orderDetail.getUnitPrice())
                            .refundUnitPrice(orderDetail.getUnitPrice())
                            .reason("")
                            .build();
                })
                .collect(Collectors.toList());

        return ReturnOrderFormDTO.builder()
                .orderId(order.getOrderID())
                .orderCode("DH-" + order.getOrderID())
                .orderStatus(order.getStatus() != null ? order.getStatus().name() : null)
                .paymentMethod(order.getPaymentMethod())
                .items(items)
                .build();
    }

    @Override
    @Transactional
    public ReturnDTO createReturn(ReturnCreateDTO request) {
        OrdersEntity order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + request.getOrderId()));

        ReturnsEntity returnEntry = ReturnsEntity.builder()
                .order(order)
                .refundAmount(BigDecimal.ZERO)
                .build();
        returnEntry = returnRepository.save(returnEntry);

        BigDecimal totalRefund = BigDecimal.ZERO;
        boolean hasReturnLine = false;
        List<ReturnOrderItemDTO> items = request.getItems() != null ? request.getItems() : List.of();

        for (ReturnOrderItemDTO item : items) {
            OrderDetailsEntity orderDetail = orderDetailRepository.findById(item.getOrderDetailId())
                    .orElseThrow(() -> new IllegalArgumentException("Order detail not found: " + item.getOrderDetailId()));

            if (!orderDetail.getOrder().getOrderID().equals(order.getOrderID())) {
                throw new IllegalArgumentException("Order detail " + item.getOrderDetailId() + " does not belong to order " + order.getOrderID());
            }

            if (item.getQuantity() == null || item.getQuantity() <= 0) {
                continue; // skip items not selected for return
            }

            hasReturnLine = true;
            int returnedQuantity = returnDetailRepository.getReturnedQuantityByOrderDetailId(orderDetail.getOrderDetailID());
            int availableToReturn = Math.max(0, orderDetail.getQuantity() - returnedQuantity);
            if (item.getQuantity() > availableToReturn) {
                throw new IllegalArgumentException("Return quantity cannot exceed available quantity for order detail " + item.getOrderDetailId());
            }

            ReturnDetailsEntity returnDetail = ReturnDetailsEntity.builder()
                    .returnEntry(returnEntry)
                    .orderDetail(orderDetail)
                    .quantity(item.getQuantity())
                    .refundUnitPrice(item.getRefundUnitPrice() != null ? item.getRefundUnitPrice() : orderDetail.getUnitPrice())
                    .reason(item.getReason())
                    .build();

            List<SaleAllocationsEntity> allocations = saleAllocationRepository
                    .findByOrderDetailOrderDetailIDOrderByAllocationID(orderDetail.getOrderDetailID());

            if (allocations.isEmpty()) {
                throw new IllegalArgumentException("No sale allocations found for order detail " + orderDetail.getOrderDetailID());
            }

            int remaining = item.getQuantity();
            for (SaleAllocationsEntity allocation : allocations) {
                if (remaining <= 0) {
                    break;
                }

                int take = Math.min(remaining, allocation.getQuantity());

                InventoryTransactionsEntity transaction = InventoryTransactionsEntity.builder()
                        .batch(allocation.getBatch())
                        .quantityChange(take)
                        .transactionType(TransactionType.RETURN)
                        .isSellable(false)
                        .referenceID(returnEntry.getReturnID())
                        .build();
                inventoryTransactionRepository.save(transaction);

                if (returnDetail.getBatch() == null) {
                    returnDetail.setBatch(allocation.getBatch());
                }

                remaining -= take;
            }

            if (remaining > 0) {
                throw new IllegalArgumentException("Return quantity exceeds allocated sold quantity for order detail " + orderDetail.getOrderDetailID());
            }

                returnDetailRepository.save(returnDetail);
            totalRefund = totalRefund.add(returnDetail.getRefundUnitPrice().multiply(BigDecimal.valueOf(returnDetail.getQuantity())));
        }

        if (!hasReturnLine) {
            returnRepository.delete(returnEntry);
            throw new IllegalArgumentException("Vui lòng chọn ít nhất một sản phẩm để trả");
        }

        returnEntry.setRefundAmount(totalRefund);
        returnRepository.save(returnEntry);

        return returnRepository.findById(returnEntry.getReturnID())
                .map(this::mapToDto)
                .orElseThrow(() -> new IllegalStateException("Created return not found"));
    }

    private ReturnDTO mapToDto(ReturnsEntity entity) {
        List<ReturnDTO.ReturnDetailDTO> details = entity.getReturnDetails().stream()
                .map(this::mapDetail)
                .collect(Collectors.toList());

        int totalItems = details.stream()
                .mapToInt(detail -> detail.getQuantity() == null ? 0 : detail.getQuantity())
                .sum();

        return ReturnDTO.builder()
                .returnId(entity.getReturnID())
                .orderId(entity.getOrder().getOrderID())
                .orderStatus(entity.getOrder().getStatus() != null ? entity.getOrder().getStatus().name() : null)
                .paymentMethod(entity.getOrder().getPaymentMethod())
                .returnDate(entity.getReturnDate())
                .refundAmount(entity.getRefundAmount())
                .totalItems(totalItems)
                .details(details)
                .build();
    }

    private ReturnDTO.ReturnDetailDTO mapDetail(ReturnDetailsEntity detail) {
        String productName = "";
        if (detail.getOrderDetail() != null && detail.getOrderDetail().getProduct() != null) {
            productName = detail.getOrderDetail().getProduct().getName();
        }

        BigDecimal unitPrice = detail.getRefundUnitPrice() != null ? detail.getRefundUnitPrice() : BigDecimal.ZERO;
        int qty = detail.getQuantity() != null ? detail.getQuantity() : 0;
        BigDecimal refundTotal = unitPrice.multiply(BigDecimal.valueOf(qty));

        return ReturnDTO.ReturnDetailDTO.builder()
                .productName(productName)
                .quantity(qty)
                .refundUnitPrice(unitPrice)
                .refundTotal(refundTotal)
                .reason(detail.getReason())
                .build();
    }
}
