package ute.fit.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import ute.fit.entity.ProductsEntity;
import ute.fit.repository.ProductRepository;

@Controller
public class ProductController {

    @Autowired
    private ProductRepository repo;

    @GetMapping("/listproduct")
    public String list(Model model, HttpServletRequest request){
        model.addAttribute("products", repo.findAll());
        model.addAttribute("currentPath", request.getRequestURI());
        return "product/list";
    }

    @GetMapping("/product/create")
    public String create(Model model, HttpServletRequest request){
        model.addAttribute("product", new ProductsEntity());
        model.addAttribute("currentPath", request.getRequestURI());
        return "product/form";
    }

    @PostMapping("/product/save")
    public String save(ProductsEntity p){
        repo.save(p);
        return "redirect:/listproduct";
    }

    @GetMapping("/product/edit/{id}")
    public String edit(@PathVariable String id, Model model, HttpServletRequest request){
        model.addAttribute("product", repo.findById(id).orElse(null));
        model.addAttribute("currentPath", request.getRequestURI());
        return "product/form";
    }

    @GetMapping("/product/delete/{id}")
    public String delete(@PathVariable String id){
        repo.deleteById(id);
        return "redirect:/listproduct";
    }

    @GetMapping("/product/detail/{id}")
    public String detail(@PathVariable String id, Model model, HttpServletRequest request){
        model.addAttribute("product", repo.findById(id).orElse(null));
        model.addAttribute("currentPath", request.getRequestURI());
        return "product/detail";
    }
}