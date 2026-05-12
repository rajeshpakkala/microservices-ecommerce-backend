package com.ecommerce.product_service.repository;

import com.ecommerce.product_service.entity.Product;
import com.ecommerce.product_service.enums.ProductStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByVendorId(String vendorId);

    List<Product> findByStatus(ProductStatus status);

    List<Product> findByCategoryIdAndStatus(Long categoryId, ProductStatus status);

    List<Product> findByVendorIdAndStatus(String vendorId, ProductStatus status);

    Page<Product> findByStatus(ProductStatus status, Pageable pageable);

    Page<Product> findByNameContainingIgnoreCaseAndStatus(String name, ProductStatus status, Pageable pageable);
}
