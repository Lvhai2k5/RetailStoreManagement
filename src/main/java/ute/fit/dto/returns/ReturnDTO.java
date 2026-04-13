package ute.fit.dto.returns;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReturnDTO {
    private Integer returnId;
    private Integer orderId;
    private String orderStatus;
    private String paymentMethod;
    private LocalDateTime returnDate;
    private BigDecimal refundAmount;
    private Integer totalItems;
    private List<ReturnDetailDTO> details;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ReturnDetailDTO {
        private Integer orderDetailId;
        private String productName;
        private Integer quantity;
        private BigDecimal refundUnitPrice;
        private BigDecimal refundTotal;
        private String reason;
    }
}
