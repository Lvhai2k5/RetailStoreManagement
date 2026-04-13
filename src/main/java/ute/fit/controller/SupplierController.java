package ute.fit.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ute.fit.dto.SupplierFormDTO;
import ute.fit.service.ISupplierService;

@Controller
@RequestMapping("/suppliers")
public class SupplierController {

    private final ISupplierService supplierService;

    public SupplierController(ISupplierService supplierService) {
        this.supplierService = supplierService;
    }

    @GetMapping
    public String listSuppliers(@RequestParam(required = false) String keyword, Model model) {
        var suppliers = supplierService.getSuppliers(keyword);
        long total = suppliers.size();
        long collab = suppliers.stream().filter(s -> s.getStatus() != null && s.getStatus().name().equals("COLLAB")).count();
        long stop = suppliers.stream().filter(s -> s.getStatus() != null && s.getStatus().name().equals("STOP")).count();

        model.addAttribute("suppliers", suppliers);
        model.addAttribute("keyword", keyword == null ? "" : keyword);
        model.addAttribute("totalSuppliers", total);
        model.addAttribute("collabSuppliers", collab);
        model.addAttribute("stopSuppliers", stop);
        return "suppliers/supplier-management";
    }

    @GetMapping("/new")
    public String newSupplier(Model model) {
        SupplierFormDTO form = new SupplierFormDTO();
        model.addAttribute("supplierForm", form);
        model.addAttribute("isEdit", false);
        return "suppliers/supplier-add";
    }

    @GetMapping("/edit/{id}")
    public String editSupplier(@PathVariable Integer id, Model model, RedirectAttributes redirectAttributes) {
        try {
            model.addAttribute("supplierForm", supplierService.getSupplierForm(id));
            model.addAttribute("isEdit", true);
            return "suppliers/supplier-add";
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/suppliers";
        }
    }

    @PostMapping("/save")
    public String saveSupplier(@ModelAttribute("supplierForm") SupplierFormDTO form,
                               Model model,
                               RedirectAttributes redirectAttributes) {
        try {
            supplierService.saveSupplier(form);
            redirectAttributes.addFlashAttribute("successMessage", "Lưu nhà cung cấp thành công.");
            return "redirect:/suppliers";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("isEdit", form.getSupplierID() != null);
            return "suppliers/supplier-add";
        }
    }

    @PostMapping("/delete/{id}")
    public String deleteSupplier(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        try {
            supplierService.deleteSupplier(id);
            redirectAttributes.addFlashAttribute("successMessage", "Xóa nhà cung cấp thành công.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/suppliers";
    }

    @PostMapping("/toggle-status/{id}")
    public String toggleStatus(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        try {
            supplierService.toggleSupplierStatus(id);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật trạng thái thành công.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/suppliers";
    }
}
