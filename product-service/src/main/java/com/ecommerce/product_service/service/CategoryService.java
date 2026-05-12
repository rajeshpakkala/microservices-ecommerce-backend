package com.ecommerce.product_service.service;

import com.ecommerce.product_service.dto.ApiResponse;
import com.ecommerce.product_service.dto.CategoryRequest;
import com.ecommerce.product_service.dto.CategoryResponse;
import com.ecommerce.product_service.entity.Category;
import com.ecommerce.product_service.repository.CategoryRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public ApiResponse<CategoryResponse> createCategory(CategoryRequest request) {

        if (categoryRepository.existsByName(request.getName())) {
            throw new IllegalArgumentException(
                    "Category already exists with name: " + request.getName());
        }

        Category category = Category.builder()
                .name(request.getName())
                .description(request.getDescription())
                .build();

        Category saved = categoryRepository.save(category);

        return ApiResponse.<CategoryResponse>builder()
                .responseCode(201)
                .responseMessage("Category created successfully")
                .success(true)
                .responseData(mapToResponse(saved))
                .build();
    }

    public ApiResponse<List<CategoryResponse>> getAllCategories() {

        List<CategoryResponse> categories = categoryRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();

        return ApiResponse.<List<CategoryResponse>>builder()
                .responseCode(200)
                .responseMessage("Categories fetched successfully")
                .success(true)
                .responseData(categories)
                .build();
    }

    public ApiResponse<String> deleteCategory(Long id) {

        Category category = categoryRepository.findById(id)
                .orElseThrow(() ->
                        new NoSuchElementException("Category not found with id: " + id));

        categoryRepository.delete(category);

        return ApiResponse.<String>builder()
                .responseCode(200)
                .responseMessage("Category deleted successfully")
                .success(true)
                .responseData("Category with id " + id + " deleted")
                .build();
    }

    private CategoryResponse mapToResponse(Category category) {
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .description(category.getDescription())
                .build();
    }
}
