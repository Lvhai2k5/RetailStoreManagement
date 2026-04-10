package ute.fit.controller;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;
import ute.fit.dto.OrderDTO;
import ute.fit.dto.OrderItemDTO;
import ute.fit.dto.ProductDTO;
import ute.fit.entity.*;
import ute.fit.model.OrderStatus;
import ute.fit.repository.*;
import ute.fit.service.OrderService;

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
    private OrderService orderService;

    // ================= CREATE =================
    @GetMapping("/create")
    public String create(Model model){

        List<ProductDTO> list = productRepo.findAll().stream().map(p -> {
            ProductDTO dto = new ProductDTO();
            dto.setProductID(p.getProductID());
            dto.setName(p.getName());
            dto.setPrice(p.getDefaultSellingPrice());
            return dto;
        }).toList();

        model.addAttribute("orderDTO", new OrderDTO());
        model.addAttribute("products", list);

        return "order/create";
    }

    // ================= SAVE ORDER (PENDING) =================
    @PostMapping("/save")
    public String saveOrder(@ModelAttribute OrderDTO orderDTO,
                            HttpSession session) {

        // lưu DB (Pending)
        OrdersEntity order = orderService.createOrder(orderDTO);

        // lưu ID để dùng cho payment
        session.setAttribute("orderId", order.getOrderID());

        return "redirect:/order/payment";
    }

    // ================= PAYMENT PAGE =================
    @GetMapping("/payment")
    public String paymentPage() {
        return "order/payment";
    }

    // ================= CONFIRM PAYMENT =================
    @PostMapping("/pay")
    public String pay(@RequestParam String paymentMethod,
                      HttpSession session) {

        Integer orderId = (Integer) session.getAttribute("orderId");

        if(orderId == null){
            return "redirect:/order/create";
        }

        orderService.updatePayment(orderId, paymentMethod);

        session.removeAttribute("orderId");

        return "redirect:/dashboard";
    }
}

//
//@Controller
//@RequestMapping("/order")
//public class OrderController {
//
//    @Autowired
//    private ProductRepository productRepo;
//    
//    @Autowired
//    private OrderService orderService;
//
//    @GetMapping("/create")
//    public String create(Model model){
//
//        List<ProductDTO> list = productRepo.findAll().stream().map(p -> {
//            ProductDTO dto = new ProductDTO();
//            dto.setProductID(p.getProductID());
//            dto.setName(p.getName());
//            dto.setPrice(p.getDefaultSellingPrice());
//            return dto;
//        }).toList();
//
//        model.addAttribute("orderDTO", new OrderDTO());
//        model.addAttribute("products", list);
//
//        return "order/create";
//    }
//    
//    @PostMapping("/payment")
//    public String goToPayment(@ModelAttribute OrderDTO orderDTO, HttpSession session) {
//        session.setAttribute("cart", orderDTO);
//        return "order/payment";
//    }
//
//    @PostMapping("/save")
//    public String saveOrder(HttpSession session) {
//        OrderDTO orderDTO = (OrderDTO) session.getAttribute("cart");
//
//        orderService.createOrder(orderDTO);
//
//        session.removeAttribute("cart");
//
//        return "redirect:/payment";
//    }
////    @PostMapping("/save")
////    public String saveOrder(@ModelAttribute OrderDTO orderDTO) {
////
////        orderService.createOrder(orderDTO);
////
////        return "redirect:/payment"; 
////    }
//}