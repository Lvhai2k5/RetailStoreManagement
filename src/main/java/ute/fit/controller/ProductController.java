package ute.fit.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import ute.fit.entity.ProductTypesMarkupEntity;
import ute.fit.entity.ProductsEntity;
import ute.fit.repository.ProductRepository;
import ute.fit.repository.ProductTypeRepository;
import ute.fit.service.ProductService;

@Controller
public class ProductController {

    @Autowired
    private ProductRepository repo;
    
    @Autowired
    ProductTypeRepository productTypeRepo;

    @Autowired
    ProductService productService;

    @GetMapping("/listproduct")
    public String list(Model model, HttpServletRequest request){
        model.addAttribute("products", repo.findAll());
        model.addAttribute("currentPath", request.getRequestURI());
        return "product/list";
    }

    @GetMapping("/product/create")
    public String create(Model model){
        model.addAttribute("product", new ProductsEntity());
        model.addAttribute("productTypes", productTypeRepo.findAll()); 
        return "product/create";
    }

    @PostMapping("/product/save")
    public String save(ProductsEntity p){

        if(p.getProductType() != null 
           && p.getProductType().getProductTypeID() != null){

            ProductTypesMarkupEntity type = productTypeRepo
                    .findById(p.getProductType().getProductTypeID())
                    .orElse(null);

            p.setProductType(type); // khúc này gán lại enti từ db nha Vũ hải đừng quên dùm cái
        }

        repo.save(p);
        return "redirect:/listproduct";
    }

    @GetMapping("/product/edit/{id}")
    public String edit(@PathVariable String id, Model model){

        model.addAttribute("product", repo.findById(id).orElse(new ProductsEntity()));
        model.addAttribute("productTypes", productTypeRepo.findAll()); 

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
    @GetMapping("/product/max-stock/{id}")
    @ResponseBody
    public int getMaxStock(@PathVariable String id){
        return productService.getMaxStock(id);
    }
}