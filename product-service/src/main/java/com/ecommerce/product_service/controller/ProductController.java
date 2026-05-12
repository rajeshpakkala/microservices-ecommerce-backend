package com.ecommerce.product_service.controller;

import com.ecommerce.product_service.dto.*;
import com.ecommerce.product_service.service.ProductService;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/ecommerce/api/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    // ===================== PUBLIC APIs =====================

    @GetMapping("/fetch/all")
    public ResponseEntity<ApiResponse<Page<ProductResponse>>> getAllProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        return ResponseEntity.ok(productService.getAllProducts(page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> getProductById(
            @PathVariable Long id) {

        return ResponseEntity.ok(productService.getProductById(id));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<Page<ProductResponse>>> searchProducts(
            @RequestParam String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        return ResponseEntity.ok(productService.searchProducts(q, page, size));
    }

    @GetMapping("/category/{categoryId}")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getProductsByCategory(
            @PathVariable Long categoryId) {

        return ResponseEntity.ok(productService.getProductsByCategory(categoryId));
    }

    @GetMapping("/vendor/{vendorId}")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getProductsByVendor(
            @PathVariable String vendorId) {

        return ResponseEntity.ok(productService.getProductsByVendor(vendorId));
    }

    // ===================== VENDOR APIs =====================

    @PreAuthorize("hasRole('VENDOR')")
    @PostMapping
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(
            @RequestBody ProductRequest request) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(productService.createProduct(request));
    }

    @PreAuthorize("hasRole('VENDOR')")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> updateProduct(
            @PathVariable Long id,
            @RequestBody ProductRequest request) {

        return ResponseEntity.ok(productService.updateProduct(id, request));
    }

    @PreAuthorize("hasRole('VENDOR')")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> deleteProduct(
            @PathVariable Long id) {

        return ResponseEntity.ok(productService.deleteProduct(id));
    }

    @PreAuthorize("hasRole('VENDOR')")
    @GetMapping("/my-products")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getMyProducts() {

        return ResponseEntity.ok(productService.getMyProducts());
    }

    @PreAuthorize("hasRole('VENDOR')")
    @PatchMapping("/{id}/stock")
    public ResponseEntity<ApiResponse<ProductResponse>> updateStock(
            @PathVariable Long id,
            @RequestBody StockUpdateRequest request) {

        return ResponseEntity.ok(productService.updateStock(id, request));
    }

    // ===================== ADMIN APIs =====================

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/all")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> adminGetAllProducts() {

        return ResponseEntity.ok(productService.adminGetAllProducts());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/pending")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getPendingProducts() {

        return ResponseEntity.ok(productService.getPendingProducts());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/admin/{id}/approve")
    public ResponseEntity<ApiResponse<ProductResponse>> approveProduct(
            @PathVariable Long id) {

        return ResponseEntity.ok(productService.approveProduct(id));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/admin/{id}/reject")
    public ResponseEntity<ApiResponse<ProductResponse>> rejectProduct(
            @PathVariable Long id) {

        return ResponseEntity.ok(productService.rejectProduct(id));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/admin/{id}")
    public ResponseEntity<ApiResponse<String>> adminDeleteProduct(
            @PathVariable Long id) {

        return ResponseEntity.ok(productService.adminDeleteProduct(id));
    }
}
