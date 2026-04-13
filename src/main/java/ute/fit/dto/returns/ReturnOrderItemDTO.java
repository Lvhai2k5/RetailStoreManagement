package ute.fit.dto.returns;

import java.math.BigDecimal;

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
public class ReturnOrderItemDTO {
    private Integer orderDetailId;
    private String productName;
    private Integer quantity;
    private Integer returnedQuantity;
    private Integer availableQuantity;
    private BigDecimal unitPrice;
    private BigDecimal refundUnitPrice;
    private String reason;
}
