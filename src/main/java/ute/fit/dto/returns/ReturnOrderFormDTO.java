package ute.fit.dto.returns;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ute.fit.dto.returns.ReturnOrderItemDTO;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReturnOrderFormDTO {
    private Integer orderId;
    private String orderCode;
    private String orderStatus;
    private String paymentMethod;
    private List<ReturnOrderItemDTO> items;
}
