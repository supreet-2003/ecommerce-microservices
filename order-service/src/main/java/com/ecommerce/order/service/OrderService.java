package com.ecommerce.order.service;

import com.ecommerce.order.client.CartClient;
import com.ecommerce.order.client.ProductClient;
import com.ecommerce.order.dto.*;
import com.ecommerce.order.entity.Order;
import com.ecommerce.order.entity.OrderItem;
import com.ecommerce.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductClient productClient;
    private final CartClient cartClient;

    @Transactional
    public OrderResponse placeOrder(Long userId) {
        // 1. Fetch user's cart via Feign
        CartResponse cart = cartClient.getCart(userId);
        if (cart == null || cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new RuntimeException("Cart is empty. Cannot place order.");
        }

        Order order = Order.builder()
                .userId(userId)
                .orderDate(LocalDateTime.now())
                .status("PLACED")
                .items(new ArrayList<>())
                .build();

        BigDecimal totalAmount = BigDecimal.ZERO;

        // 2. Validate products, deduct stock, and construct OrderItems
        for (CartItemResponse item : cart.getItems()) {
            ProductResponse product = productClient.getProductById(item.getProductId());
            if (product == null) {
                throw new RuntimeException("Product not found with id: " + item.getProductId());
            }

            // Deduct stock via Feign
            productClient.deductStock(product.getId(), item.getQuantity());

            BigDecimal itemTotal = product.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
            totalAmount = totalAmount.add(itemTotal);

            OrderItem orderItem = OrderItem.builder()
                    .productId(product.getId())
                    .productName(product.getName())
                    .price(product.getPrice())
                    .quantity(item.getQuantity())
                    .order(order)
                    .build();

            order.getItems().add(orderItem);
        }

        order.setTotalAmount(totalAmount);

        // 3. Save order to database
        Order savedOrder = orderRepository.save(order);

        // 4. Clear the cart via Feign
        cartClient.clearCart(userId);

        return mapToResponse(savedOrder);
    }

    public List<OrderResponse> getOrdersByUser(Long userId) {
        return orderRepository.findByUserId(userId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private OrderResponse mapToResponse(Order order) {
        return OrderResponse.builder()
                .id(order.getId())
                .userId(order.getUserId())
                .orderDate(order.getOrderDate())
                .status(order.getStatus())
                .totalAmount(order.getTotalAmount())
                .items(order.getItems().stream()
                        .map(item -> OrderItemResponse.builder()
                                .id(item.getId())
                                .productId(item.getProductId())
                                .productName(item.getProductName())
                                .quantity(item.getQuantity())
                                .price(item.getPrice())
                                .build())
                        .collect(Collectors.toList()))
                .build();
    }
}
