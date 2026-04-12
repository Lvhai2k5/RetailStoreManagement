package ute.fit.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ute.fit.repository.ProductRepository;

@Service
public class ProductService
{
    @Autowired
    private ProductRepository repository;

    public int getMaxStock(String productId){
        return repository.getMaxStock(productId);
    }
}