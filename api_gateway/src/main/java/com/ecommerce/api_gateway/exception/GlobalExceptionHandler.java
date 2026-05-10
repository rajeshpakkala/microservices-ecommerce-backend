package com.ecommerce.api_gateway.exception;

import com.ecommerce.api_gateway.dto.ApiResponse;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<String>>
    handleException(Exception ex) {

        ApiResponse<String> response =
                ApiResponse.<String>builder()
                        .responseCode(
                                HttpStatus.INTERNAL_SERVER_ERROR.value())
                        .responseMessage(ex.getMessage())
                        .success(false)
                        .responseData(null)
                        .build();

        return ResponseEntity
                .status(
                        HttpStatus.INTERNAL_SERVER_ERROR)
                .body(response);
    }
}
