package com.fynxt.service;

import com.fynxt.dto.OrderResponse;
import com.fynxt.dto.PlaceOrderRequest;

public interface OrderService {

    OrderResponse placeOrder(PlaceOrderRequest request);

    OrderResponse fillOrder(Long orderId);

    OrderResponse cancelOrder(Long orderId);
}