package ute.fit.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.CallableStatementCallback;
import org.springframework.jdbc.core.CallableStatementCreator;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.Types;

@Repository
@RequiredArgsConstructor
public class OrderJdbcRepository {

    private final JdbcTemplate jdbcTemplate;

    // ================= CREATE ORDER =================
    public Integer createOrder(boolean isImmediate) {

        return jdbcTemplate.execute(
                (CallableStatementCreator) connection -> {
                    CallableStatement cs = connection.prepareCall("{call PROC_CreateOrder(?, ?)}");
                    cs.setBoolean(1, isImmediate);
                    cs.registerOutParameter(2, Types.INTEGER);
                    return cs;
                },
                (CallableStatementCallback<Integer>) cs -> {
                    cs.execute();
                    return cs.getInt(2);
                }
        );
    }

    // ================= ADD ORDER ITEM =================
    public void addOrderItem(Integer orderId, String productId, Integer quantity) {

        jdbcTemplate.update(
                "EXEC PROC_AddOrderItem ?, ?, ?",
                orderId,
                productId,
                quantity
        );
    }

    // ================= UPDATE ORDER ITEM =================
    public void updateOrderItem(Integer orderDetailId, Integer newQuantity) {

        jdbcTemplate.update(
                "EXEC PROC_UpdateOrderItem ?, ?",
                orderDetailId,
                newQuantity
        );
    }

    // ================= REMOVE ORDER ITEM =================
    public void removeOrderDetail(Integer orderDetailId) {

        jdbcTemplate.update(
                "EXEC PROC_RemoveOrderItem ?",
                orderDetailId
        );
    }

    // ================= PAY ORDER =================
    public void payOrder(Integer orderId) {

        jdbcTemplate.update(
                "EXEC PROC_PayOrder ?",
                orderId
        );
    }

    // ================= CANCEL ORDER =================
    public void cancelOrder(Integer orderId) {

        jdbcTemplate.update(
                "EXEC PROC_CancelOrder ?",
                orderId
        );
    }

    // ================= UPDATE TOTAL =================
    public void updateTotal(Integer orderId, BigDecimal total) {

        jdbcTemplate.update(
                "UPDATE Orders SET TotalAmount = ? WHERE OrderID = ?",
                total,
                orderId
        );
    }

    // ================= UPDATE PAYMENT METHOD =================
    public void updatePaymentMethod(Integer orderId, String method) {

        jdbcTemplate.update(
                "UPDATE Orders SET PaymentMethod = ? WHERE OrderID = ?",
                method,
                orderId
        );
    }
}