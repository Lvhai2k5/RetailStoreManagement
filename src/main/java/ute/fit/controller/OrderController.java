package ute.fit.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import ute.fit.dto.OrderDTO;
import ute.fit.dto.OrderItemDTO;
import ute.fit.dto.ProductDTO;
import ute.fit.entity.*;
import ute.fit.model.OrderStatus;
import ute.fit.repository.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/order")
public class OrderController {

    @Autowired
    private ProductRepository productRepo;

    @Autowired
    private OrderRepository orderRepo;

    @Autowired
    private OrderDetailRepository orderDetailRepo;

    @GetMapping("/create")
    public String create(Model model){

        List<ProductDTO> list = productRepo.findAll().stream().map(p -> {
            ProductDTO dto = new ProductDTO();
            dto.setProductID(p.getProductID());
            dto.setName(p.getName());
            dto.setPrice(p.getDefaultSellingPrice());
            return dto;
        }).toList();

        model.addAttribute("order", new OrderDTO());
        model.addAttribute("products", list);

        return "order/create";
    }

    @PostMapping("/save")
    public String save(@ModelAttribute OrderDTO dto){

        OrdersEntity order = new OrdersEntity();
        order.setPaymentMethod(dto.getPaymentMethod());
        order.setStatus(OrderStatus.Pending);

        List<OrderDetailsEntity> details = new ArrayList<>();

        for(OrderItemDTO item : dto.getItems()){
            ProductsEntity product = productRepo.findById(item.getProductID()).orElse(null);

            if(product != null){
                OrderDetailsEntity d = new OrderDetailsEntity();
                d.setOrder(order);
                d.setProduct(product);
                d.setQuantity(item.getQuantity());
                d.setUnitPrice(product.getDefaultSellingPrice());

                details.add(d);
            }
        }

        order.setOrderDetails(details);

        orderRepo.save(order);

        return "redirect:/dashboard";
    }
}