package ute.fit.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import ute.fit.entity.ProductPriceHistoryEntity;
import ute.fit.entity.ProductTypesMarkupEntity;
import ute.fit.entity.ProductsEntity;
import ute.fit.repository.ProductPriceHistoryRepository;
import ute.fit.repository.ProductRepository;
import ute.fit.repository.ProductTypeRepository;
import ute.fit.service.ProductService;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Controller
public class ProductController {

    @Autowired
    private ProductRepository repo;
    
    @Autowired
    ProductTypeRepository productTypeRepo;

    @Autowired
    ProductService productService;

    // TIÊM THÊM REPOSITORY LỊCH SỬ GIÁ VÀO ĐÂY
    @Autowired
    private ProductPriceHistoryRepository priceHistoryRepo;

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

        // 1. NẾU GIÁ NULL (Do bỏ nhập ở Form), GÁN MẶC ĐỊNH LÀ 0
        if (p.getDefaultSellingPrice() == null) {
            p.setDefaultSellingPrice(BigDecimal.ZERO);
        }

        // 2. LƯU SẢN PHẨM VÀO BẢNG Products
        repo.save(p);

        // 3. TỰ ĐỘNG GHI LOG VÀO BẢNG LỊCH SỬ GIÁ
        ProductPriceHistoryEntity history = new ProductPriceHistoryEntity();
        history.setProduct(p);
        history.setSellingPrice(p.getDefaultSellingPrice());
        history.setChangeReason("Market changes"); // Loại thay đổi theo yêu cầu của bạn
        history.setEffectiveDate(LocalDateTime.now());
        
        priceHistoryRepo.save(history);

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