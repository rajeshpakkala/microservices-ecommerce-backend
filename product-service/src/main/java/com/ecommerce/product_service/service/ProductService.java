package com.ecommerce.product_service.service;

import com.ecommerce.product_service.dto.ApiResponse;
import com.ecommerce.product_service.dto.ProductRequest;
import com.ecommerce.product_service.dto.ProductResponse;
import com.ecommerce.product_service.dto.StockUpdateRequest;
import com.ecommerce.product_service.entity.Category;
import com.ecommerce.product_service.entity.Product;
import com.ecommerce.product_service.enums.ProductStatus;
import com.ecommerce.product_service.repository.CategoryRepository;
import com.ecommerce.product_service.repository.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    public ProductService(ProductRepository productRepository,
                          CategoryRepository categoryRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
    }

    // ===================== VENDOR OPERATIONS =====================

    public ApiResponse<ProductResponse> createProduct(ProductRequest request) {

        String vendorId = getCurrentUsername();

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() ->
                        new NoSuchElementException("Category not found with id: " + request.getCategoryId()));

        Product product = Product.builder()
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .stock(request.getStock())
                .category(category)
                .vendorId(vendorId)
                .status(ProductStatus.PENDING)
                .imageUrl(request.getImageUrl())
                .build();

        Product saved = productRepository.save(product);

        return ApiResponse.<ProductResponse>builder()
                .responseCode(201)
                .responseMessage("Product created successfully. Pending admin approval.")
                .success(true)
                .responseData(mapToResponse(saved))
                .build();
    }

    public ApiResponse<ProductResponse> updateProduct(Long id, ProductRequest request) {

        String vendorId = getCurrentUsername();

        Product product = productRepository.findById(id)
                .orElseThrow(() ->
                        new NoSuchElementException("Product not found with id: " + id));

        if (!product.getVendorId().equals(vendorId)) {
            throw new IllegalArgumentException("You can only update your own products");
        }

        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() ->
                            new NoSuchElementException("Category not found with id: " + request.getCategoryId()));
            product.setCategory(category);
        }

        if (request.getName() != null) product.setName(request.getName());
        if (request.getDescription() != null) product.setDescription(request.getDescription());
        if (request.getPrice() != null) product.setPrice(request.getPrice());
        if (request.getStock() != null) product.setStock(request.getStock());
        if (request.getImageUrl() != null) product.setImageUrl(request.getImageUrl());

        product.setStatus(ProductStatus.PENDING);

        Product saved = productRepository.save(product);

        return ApiResponse.<ProductResponse>builder()
                .responseCode(200)
                .responseMessage("Product updated successfully. Pending re-approval.")
                .success(true)
                .responseData(mapToResponse(saved))
                .build();
    }

    public ApiResponse<String> deleteProduct(Long id) {

        String vendorId = getCurrentUsername();

        Product product = productRepository.findById(id)
                .orElseThrow(() ->
                        new NoSuchElementException("Product not found with id: " + id));

        if (!product.getVendorId().equals(vendorId)) {
            throw new IllegalArgumentException("You can only delete your own products");
        }

        productRepository.delete(product);

        return ApiResponse.<String>builder()
                .responseCode(200)
                .responseMessage("Product deleted successfully")
                .success(true)
                .responseData("Product with id " + id + " deleted")
                .build();
    }

    public ApiResponse<List<ProductResponse>> getMyProducts() {

        String vendorId = getCurrentUsername();

        List<ProductResponse> products = productRepository.findByVendorId(vendorId)
                .stream()
                .map(this::mapToResponse)
                .toList();

        return ApiResponse.<List<ProductResponse>>builder()
                .responseCode(200)
                .responseMessage("Products fetched successfully")
                .success(true)
                .responseData(products)
                .build();
    }

    public ApiResponse<ProductResponse> updateStock(Long id, StockUpdateRequest request) {

        String vendorId = getCurrentUsername();

        Product product = productRepository.findById(id)
                .orElseThrow(() ->
                        new NoSuchElementException("Product not found with id: " + id));

        if (!product.getVendorId().equals(vendorId)) {
            throw new IllegalArgumentException("You can only update stock of your own products");
        }

        if (request.getQuantity() < 0) {
            throw new IllegalArgumentException("Stock quantity cannot be negative");
        }

        product.setStock(request.getQuantity());
        Product saved = productRepository.save(product);

        return ApiResponse.<ProductResponse>builder()
                .responseCode(200)
                .responseMessage("Stock updated successfully")
                .success(true)
                .responseData(mapToResponse(saved))
                .build();
    }

    // ===================== PUBLIC OPERATIONS =====================

    public ApiResponse<Page<ProductResponse>> getAllProducts(int page, int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Page<ProductResponse> products = productRepository
                .findByStatus(ProductStatus.APPROVED, pageable)
                .map(this::mapToResponse);

        return ApiResponse.<Page<ProductResponse>>builder()
                .responseCode(200)
                .responseMessage("Products fetched successfully")
                .success(true)
                .responseData(products)
                .build();
    }

    public ApiResponse<ProductResponse> getProductById(Long id) {

        Product product = productRepository.findById(id)
                .orElseThrow(() ->
                        new NoSuchElementException("Product not found with id: " + id));

        if (product.getStatus() != ProductStatus.APPROVED) {
            throw new NoSuchElementException("Product not found with id: " + id);
        }

        return ApiResponse.<ProductResponse>builder()
                .responseCode(200)
                .responseMessage("Product fetched successfully")
                .success(true)
                .responseData(mapToResponse(product))
                .build();
    }

    public ApiResponse<Page<ProductResponse>> searchProducts(String query, int page, int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Page<ProductResponse> products = productRepository
                .findByNameContainingIgnoreCaseAndStatus(query, ProductStatus.APPROVED, pageable)
                .map(this::mapToResponse);

        return ApiResponse.<Page<ProductResponse>>builder()
                .responseCode(200)
                .responseMessage("Search results fetched successfully")
                .success(true)
                .responseData(products)
                .build();
    }

    public ApiResponse<List<ProductResponse>> getProductsByCategory(Long categoryId) {

        categoryRepository.findById(categoryId)
                .orElseThrow(() ->
                        new NoSuchElementException("Category not found with id: " + categoryId));

        List<ProductResponse> products = productRepository
                .findByCategoryIdAndStatus(categoryId, ProductStatus.APPROVED)
                .stream()
                .map(this::mapToResponse)
                .toList();

        return ApiResponse.<List<ProductResponse>>builder()
                .responseCode(200)
                .responseMessage("Products by category fetched successfully")
                .success(true)
                .responseData(products)
                .build();
    }

    public ApiResponse<List<ProductResponse>> getProductsByVendor(String vendorId) {

        List<ProductResponse> products = productRepository
                .findByVendorIdAndStatus(vendorId, ProductStatus.APPROVED)
                .stream()
                .map(this::mapToResponse)
                .toList();

        return ApiResponse.<List<ProductResponse>>builder()
                .responseCode(200)
                .responseMessage("Products by vendor fetched successfully")
                .success(true)
                .responseData(products)
                .build();
    }

    // ===================== ADMIN OPERATIONS =====================

    public ApiResponse<List<ProductResponse>> adminGetAllProducts() {

        List<ProductResponse> products = productRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();

        return ApiResponse.<List<ProductResponse>>builder()
                .responseCode(200)
                .responseMessage("All products fetched successfully")
                .success(true)
                .responseData(products)
                .build();
    }

    public ApiResponse<List<ProductResponse>> getPendingProducts() {

        List<ProductResponse> products = productRepository
                .findByStatus(ProductStatus.PENDING)
                .stream()
                .map(this::mapToResponse)
                .toList();

        return ApiResponse.<List<ProductResponse>>builder()
                .responseCode(200)
                .responseMessage("Pending products fetched successfully")
                .success(true)
                .responseData(products)
                .build();
    }

    public ApiResponse<ProductResponse> approveProduct(Long id) {

        Product product = productRepository.findById(id)
                .orElseThrow(() ->
                        new NoSuchElementException("Product not found with id: " + id));

        product.setStatus(ProductStatus.APPROVED);
        Product saved = productRepository.save(product);

        return ApiResponse.<ProductResponse>builder()
                .responseCode(200)
                .responseMessage("Product approved successfully")
                .success(true)
                .responseData(mapToResponse(saved))
                .build();
    }

    public ApiResponse<ProductResponse> rejectProduct(Long id) {

        Product product = productRepository.findById(id)
                .orElseThrow(() ->
                        new NoSuchElementException("Product not found with id: " + id));

        product.setStatus(ProductStatus.REJECTED);
        Product saved = productRepository.save(product);

        return ApiResponse.<ProductResponse>builder()
                .responseCode(200)
                .responseMessage("Product rejected successfully")
                .success(true)
                .responseData(mapToResponse(saved))
                .build();
    }

    public ApiResponse<String> adminDeleteProduct(Long id) {

        Product product = productRepository.findById(id)
                .orElseThrow(() ->
                        new NoSuchElementException("Product not found with id: " + id));

        productRepository.delete(product);

        return ApiResponse.<String>builder()
                .responseCode(200)
                .responseMessage("Product deleted successfully")
                .success(true)
                .responseData("Product with id " + id + " deleted by admin")
                .build();
    }

    // ===================== HELPER =====================

    private String getCurrentUsername() {
        return SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();
    }

    private ProductResponse mapToResponse(Product product) {
        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .stock(product.getStock())
                .categoryId(product.getCategory() != null ? product.getCategory().getId() : null)
                .categoryName(product.getCategory() != null ? product.getCategory().getName() : null)
                .vendorId(product.getVendorId())
                .status(product.getStatus())
                .imageUrl(product.getImageUrl())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }
}
