package com.ecommerce.payment_service.exception;

import com.ecommerce.payment_service.dto.ApiResponse;
import feign.FeignException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.NoSuchElementException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(ApiResponse.<Void>builder()
                .responseCode(400)
                .responseMessage(ex.getMessage())
                .success(false)
                .build());
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(NoSuchElementException ex) {
        return ResponseEntity.status(404).body(ApiResponse.<Void>builder()
                .responseCode(404)
                .responseMessage(ex.getMessage())
                .success(false)
                .build());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(403).body(ApiResponse.<Void>builder()
                .responseCode(403)
                .responseMessage("Access denied")
                .success(false)
                .build());
    }

    @ExceptionHandler(FeignException.class)
    public ResponseEntity<ApiResponse<Void>> handleFeignException(FeignException ex) {
        return ResponseEntity.status(502).body(ApiResponse.<Void>builder()
                .responseCode(502)
                .responseMessage("Downstream service error: " + ex.getMessage())
                .success(false)
                .build());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneral(Exception ex) {
        return ResponseEntity.status(500).body(ApiResponse.<Void>builder()
                .responseCode(500)
                .responseMessage("Internal server error: " + ex.getMessage())
                .success(false)
                .build());
    }
}
