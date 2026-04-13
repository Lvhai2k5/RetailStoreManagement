package ute.fit.controller;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import lombok.RequiredArgsConstructor;
import ute.fit.dto.returns.ReturnCreateDTO;
import ute.fit.dto.returns.ReturnDTO;
import ute.fit.dto.returns.ReturnOrderFormDTO;
import ute.fit.dto.returns.ReturnOrderSummaryDTO;
import ute.fit.service.IReturnService;

@Controller
@RequestMapping("/return")
@RequiredArgsConstructor
public class ReturnController {

    private final IReturnService returnService;

    @GetMapping
    public String listReturns(@RequestParam(defaultValue = "0") int page,
                              @RequestParam(defaultValue = "10") int size,
                              Model model) {
        Pageable pageable = PageRequest.of(page, size);
        Page<ReturnDTO> returnsPage = returnService.getAllReturns(pageable);

        long pendingCount = returnsPage.getContent().stream()
                .filter(r -> "Pending".equalsIgnoreCase(r.getOrderStatus()))
                .count();
        long completedCount = returnsPage.getContent().stream()
                .filter(r -> "Paid".equalsIgnoreCase(r.getOrderStatus()))
                .count();
        BigDecimal totalRefund = returnsPage.getContent().stream()
                .map(r -> r.getRefundAmount() == null ? BigDecimal.ZERO : r.getRefundAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        String returnRate = returnsPage.getTotalElements() > 0
                ? String.format("%.1f", (pendingCount * 100.0 / returnsPage.getTotalElements()))
                : "0.0";

        model.addAttribute("returns", returnsPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", returnsPage.getTotalPages());
        model.addAttribute("totalElements", returnsPage.getTotalElements());
        model.addAttribute("pageSize", size);
        model.addAttribute("pendingCount", pendingCount);
        model.addAttribute("completedCount", completedCount);
        model.addAttribute("totalRefund", totalRefund);
        model.addAttribute("returnRate", returnRate);

        return "return/list";
    }

    @GetMapping("/create")
    public String createReturnForm(@RequestParam(required = false) Integer orderId, Model model) {
        if (!model.containsAttribute("returnCreateDTO")) {
            model.addAttribute("returnCreateDTO", new ReturnCreateDTO());
        }
        model.addAttribute("orders", returnService.getPaidOrders());

        if (orderId != null) {
            ReturnOrderFormDTO returnForm = returnService.getReturnOrderForm(orderId);
            model.addAttribute("returnForm", returnForm);
            model.addAttribute("selectedOrderId", orderId);
        }

        return "return/create";
    }

    @GetMapping("/detail/{id}")
    public String viewReturnDetail(@PathVariable("id") Integer returnId, Model model) {
        ReturnDTO returnInfo = returnService.getReturnById(returnId);
        if (returnInfo == null) {
            return "redirect:/return";
        }
        model.addAttribute("returnInfo", returnInfo);
        return "return/detail";
    }

    @PostMapping("/create")
    public String createReturn(@ModelAttribute ReturnCreateDTO request, RedirectAttributes redirectAttributes) {
        try {
            ReturnDTO created = returnService.createReturn(request);
            redirectAttributes.addFlashAttribute("success", "Phiếu trả hàng đã được tạo thành công!");
            return "redirect:/return/detail/" + created.getReturnId();
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage() != null ? e.getMessage() : "Lỗi tạo phiếu trả hàng");
            redirectAttributes.addFlashAttribute("returnCreateDTO", request);
            if (request.getOrderId() != null) {
                return "redirect:/return/create?orderId=" + request.getOrderId();
            }
            return "redirect:/return/create";
        }
    }
}
