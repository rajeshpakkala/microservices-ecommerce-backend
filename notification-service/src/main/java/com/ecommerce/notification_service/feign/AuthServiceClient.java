package com.ecommerce.notification_service.feign;

import com.ecommerce.notification_service.dto.UserInternalResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "AUTH-SERVICE", url = "${auth.service.url:}")
public interface AuthServiceClient {

    @GetMapping("/ecommerce/api/auth/internal/users/{username}")
    UserInternalResponse getUserByUsername(@PathVariable("username") String username);
}
