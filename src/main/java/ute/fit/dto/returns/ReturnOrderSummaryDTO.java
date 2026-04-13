package ute.fit.dto.returns;

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
public class ReturnOrderSummaryDTO {
    private Integer orderId;
    private String orderCode;
    private String orderStatus;
    private String paymentMethod;
}
