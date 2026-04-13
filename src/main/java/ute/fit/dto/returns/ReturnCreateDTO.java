package ute.fit.dto.returns;

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
public class ReturnCreateDTO {
    private Integer orderId;
    private List<ReturnOrderItemDTO> items;
}
