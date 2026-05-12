package com.ecommerce.product_service.controller;

import com.ecommerce.product_service.dto.ProductInternalResponse;
import com.ecommerce.product_service.entity.Product;
import com.ecommerce.product_service.repository.ProductRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.NoSuchElementException;

@RestController
@RequestMapping("/ecommerce/api/products/internal")
public class InternalProductController {

    private final ProductRepository productRepository;

    public InternalProductController(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductInternalResponse> getProductDetails(@PathVariable Long id) {

        Product product = productRepository.findById(id)
                .orElseThrow(() ->
                        new NoSuchElementException("Product not found with id: " + id));

        ProductInternalResponse response = ProductInternalResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .price(product.getPrice())
                .stock(product.getStock())
                .status(product.getStatus().name())
                .vendorId(product.getVendorId())
                .categoryName(product.getCategory() != null ? product.getCategory().getName() : null)
                .build();

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/deduct-stock")
    public ResponseEntity<Void> deductStock(@PathVariable Long id,
                                            @RequestParam int quantity) {

        Product product = productRepository.findById(id)
                .orElseThrow(() ->
                        new NoSuchElementException("Product not found with id: " + id));

        if (product.getStock() < quantity) {
            throw new IllegalArgumentException(
                    "Insufficient stock for product: " + product.getName() +
                    ". Available: " + product.getStock() + ", Requested: " + quantity);
        }

        product.setStock(product.getStock() - quantity);
        productRepository.save(product);

        return ResponseEntity.ok().build();
    }
}
