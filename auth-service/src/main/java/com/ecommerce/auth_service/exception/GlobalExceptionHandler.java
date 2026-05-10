package com.ecommerce.auth_service.exception;

import com.ecommerce.auth_service.dto.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.NoSuchElementException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // 400 BAD REQUEST
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<String>>
    handleBadRequestException(
            IllegalArgumentException ex) {

        ApiResponse<String> response =
                new ApiResponse<>(
                        400,
                        ex.getMessage(),
                        false,
                        null
                );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response);
    }

    // 404 NOT FOUND
    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ApiResponse<String>>
    handleNotFoundException(
            NoSuchElementException ex) {

        ApiResponse<String> response =
                new ApiResponse<>(
                        404,
                        ex.getMessage(),
                        false,
                        null
                );

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(response);
    }

    // GENERAL EXCEPTION
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<String>>
    handleException(
            Exception ex) {

        ApiResponse<String> response =
                new ApiResponse<>(
                        500,
                        ex.getMessage(),
                        false,
                        null
                );

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(response);
    }
}