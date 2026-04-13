package ute.fit.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ute.fit.dto.DefectiveHandleFormDTO;
import ute.fit.service.DefectiveService;

import java.time.LocalDate;
import java.util.Map;

@Controller
@RequestMapping("/defective")
public class DefectiveController {

    private final DefectiveService defectiveService;

    public DefectiveController(DefectiveService defectiveService) {
        this.defectiveService = defectiveService;
    }

    @GetMapping
    public String management(@RequestParam(required = false) LocalDate fromDate,
                             @RequestParam(required = false) LocalDate toDate,
                             @RequestParam(required = false) String expiryStatus,
                             @RequestParam(required = false) String reason,
                             @RequestParam(required = false) String keyword,
                             Model model) {
        Map<String, Object> data = defectiveService.getManagementViewData(
                fromDate, toDate, expiryStatus, reason, keyword
        );
        model.addAllAttributes(data);
        return "defective/defective-management";
    }

    @GetMapping("/handle")
    public String handlePage(@RequestParam(required = false) Integer batchId, Model model) {
        DefectiveHandleFormDTO form = defectiveService.initHandleForm(batchId);
        model.addAttribute("form", form);
        model.addAttribute("batchInfo", defectiveService.getBatchInfoForHandle(batchId));
        return "defective/defective-handle";
    }

    @PostMapping("/handle")
    public String submitHandle(@ModelAttribute("form") DefectiveHandleFormDTO form,
                               RedirectAttributes redirectAttributes,
                               Model model) {
        try {
            defectiveService.handleDefective(form);
            redirectAttributes.addFlashAttribute("successMessage", "Ghi nhận hàng lỗi/hết hạn thành công.");
            return "redirect:/defective";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("batchInfo", defectiveService.getBatchInfoForHandle(form.getBatchID()));
            return "defective/defective-handle";
        } catch (Exception ex) {
            model.addAttribute("errorMessage", "Xử lý thất bại: " + ex.getMessage());
            model.addAttribute("batchInfo", defectiveService.getBatchInfoForHandle(form.getBatchID()));
            return "defective/defective-handle";
        }
    }

    @PostMapping("/expire-all")
    public String handleAllExpired(RedirectAttributes redirectAttributes) {
        try {
            int count = defectiveService.processAllExpiredBatches();
            redirectAttributes.addFlashAttribute("successMessage",
                    "Đã xử lý " + count + " lô hết hạn.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Xử lý thất bại: " + ex.getMessage());
        }
        return "redirect:/defective";
    }
}
