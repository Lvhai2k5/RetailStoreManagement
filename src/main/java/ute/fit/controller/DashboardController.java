package ute.fit.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import ute.fit.repository.ProductRepository;

@Controller
public class DashboardController {

    @Autowired
    private ProductRepository repo;

    @GetMapping("/dashboard")
    public String dashboard(Model model){
        model.addAttribute("totalProducts", repo.count());
        return "dashboard";
    }
}