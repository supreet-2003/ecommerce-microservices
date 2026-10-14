package com.ecommerce.order.client;

import com.ecommerce.order.dto.CartResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "cart-service")
public interface CartClient {

    @GetMapping("/api/cart/{userId}")
    CartResponse getCart(@PathVariable("userId") Long userId);

    @DeleteMapping("/api/cart/{userId}")
    void clearCart(@PathVariable("userId") Long userId);
}
