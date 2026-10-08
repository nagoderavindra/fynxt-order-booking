package com.fynxt.controller;

import com.fynxt.dto.OrderResponse;
import com.fynxt.dto.PlaceOrderRequest;
import com.fynxt.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<OrderResponse> placeOrder(
            @Valid @RequestBody PlaceOrderRequest request) {

        OrderResponse response = orderService.placeOrder(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @PostMapping("/{orderId}/fill")
    public ResponseEntity<OrderResponse> fillOrder(
            @PathVariable Long orderId) {

        OrderResponse response = orderService.fillOrder(orderId);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<OrderResponse> cancelOrder(
            @PathVariable Long orderId) {

        OrderResponse response = orderService.cancelOrder(orderId);

        return ResponseEntity.ok(response);
    }
}