package ute.fit.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import ute.fit.service.IInventoryService;

@Controller
public class InventoryController {

    @Autowired private IInventoryService inventoryService;

    @GetMapping("/inventory")
    public String showInventory(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @PageableDefault(size = 10) Pageable pageable, 
            Model model) {
        
        model.addAttribute("stats", inventoryService.getInventoryStats());
        model.addAttribute("batchPage", inventoryService.getInventoryList(keyword, status, pageable));
        
        // Giữ lại giá trị tìm kiếm trên UI
        model.addAttribute("keyword", keyword);
        model.addAttribute("currentStatus", status);
        return "Inventory/inventory-management";
    }

    @GetMapping("/inventory/trace/{id}")
    public String traceBatch(@PathVariable("id") Integer id, Model model) {
        model.addAttribute("traceData", inventoryService.getBatchTraceDetails(id));
        return "Inventory/batch-trace"; // Mở trang chi tiết
    }
}