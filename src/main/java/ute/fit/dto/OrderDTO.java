package ute.fit.dto;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;
@Getter
@Setter
public class OrderDTO {

    private String paymentMethod;

    private List<OrderItemDTO> items = new ArrayList<>();
}