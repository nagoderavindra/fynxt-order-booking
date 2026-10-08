package com.fynxt.service.impl;

import com.fynxt.dto.OrderResponse;
import com.fynxt.dto.PlaceOrderRequest;
import com.fynxt.entity.Order;
import com.fynxt.entity.OrderSide;
import com.fynxt.entity.OrderStatus;
import com.fynxt.entity.PortfolioHolding;
import com.fynxt.entity.Trader;
import com.fynxt.exception.BusinessException;
import com.fynxt.exception.ResourceNotFoundException;
import com.fynxt.repository.OrderRepository;
import com.fynxt.repository.PortfolioHoldingRepository;
import com.fynxt.repository.TraderRepository;
import com.fynxt.service.OrderService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class OrderServiceImpl implements OrderService {

    private static final int MAX_PENDING_ORDERS = 3;

    private final OrderRepository orderRepository;
    private final TraderRepository traderRepository;
    private final PortfolioHoldingRepository portfolioHoldingRepository;

    public OrderServiceImpl(
            OrderRepository orderRepository,
            TraderRepository traderRepository,
            PortfolioHoldingRepository portfolioHoldingRepository) {

        this.orderRepository = orderRepository;
        this.traderRepository = traderRepository;
        this.portfolioHoldingRepository = portfolioHoldingRepository;
    }

    @Override
    @Transactional
    public OrderResponse placeOrder(PlaceOrderRequest request) {

        Trader trader = traderRepository
                .findByIdForUpdate(request.getTraderId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Trader not found: "
                                        + request.getTraderId()
                        )
                );

        long pendingOrders = orderRepository
                .countByTraderIdAndStatus(
                        trader.getId(),
                        OrderStatus.PENDING
                );

        if (pendingOrders >= MAX_PENDING_ORDERS) {
            throw new BusinessException(
                    "Trader cannot have more than 3 pending orders"
            );
        }

        if (request.getSide() == OrderSide.SELL) {

            PortfolioHolding holding =
                    portfolioHoldingRepository
                            .findByTraderIdAndStockForUpdate(
                                    trader.getId(),
                                    request.getStock()
                            )
                            .orElseThrow(() ->
                                    new BusinessException(
                                            "Insufficient shares. "
                                                    + "No holding found for stock: "
                                                    + request.getStock()
                                    )
                            );

            int availableQuantity =
                    holding.getQuantity()
                            - holding.getReservedQuantity();

            if (availableQuantity < request.getQuantity()) {
                throw new BusinessException(
                        "Insufficient shares for SELL order. "
                                + "Available: "
                                + availableQuantity
                                + ", Requested: "
                                + request.getQuantity()
                );
            }

            holding.setReservedQuantity(
                    holding.getReservedQuantity()
                            + request.getQuantity()
            );

            holding.setUpdatedAt(LocalDateTime.now());

            portfolioHoldingRepository.save(holding);
        }

        LocalDateTime now = LocalDateTime.now();

        Order order = new Order();

        order.setTraderId(trader.getId());
        order.setStock(request.getStock());
        order.setSector(request.getSector());
        order.setQuantity(request.getQuantity());
        order.setSide(request.getSide());
        order.setStatus(OrderStatus.PENDING);
        order.setCreatedAt(now);
        order.setUpdatedAt(now);

        Order savedOrder = orderRepository.save(order);

        return toResponse(savedOrder);
    }

    @Override
    @Transactional
    public OrderResponse fillOrder(Long orderId) {

        Order existingOrder = orderRepository
                .findById(orderId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Order not found: " + orderId
                        )
                );

        traderRepository
                .findByIdForUpdate(existingOrder.getTraderId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Trader not found: "
                                        + existingOrder.getTraderId()
                        )
                );

        Order order = orderRepository
                .findByIdForUpdate(orderId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Order not found: " + orderId
                        )
                );

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new BusinessException(
                    "Order cannot be filled because current status is: "
                            + order.getStatus()
            );
        }

        PortfolioHolding holding =
                portfolioHoldingRepository
                        .findByTraderIdAndStockForUpdate(
                                order.getTraderId(),
                                order.getStock()
                        )
                        .orElse(null);

        if (order.getSide() == OrderSide.BUY) {

            if (holding == null) {

                holding = new PortfolioHolding();

                holding.setTraderId(order.getTraderId());
                holding.setStock(order.getStock());
                holding.setSector(order.getSector());
                holding.setQuantity(order.getQuantity());
                holding.setReservedQuantity(0);
                holding.setCreatedAt(LocalDateTime.now());

            } else {

                holding.setQuantity(
                        holding.getQuantity()
                                + order.getQuantity()
                );
            }

        } else {

            if (holding == null) {
                throw new BusinessException(
                        "Portfolio holding not found for SELL order"
                );
            }

            if (holding.getReservedQuantity()
                    < order.getQuantity()) {

                throw new BusinessException(
                        "Reserved quantity is insufficient "
                                + "for SELL order"
                );
            }

            holding.setQuantity(
                    holding.getQuantity()
                            - order.getQuantity()
            );

            holding.setReservedQuantity(
                    holding.getReservedQuantity()
                            - order.getQuantity()
            );
        }

        holding.setUpdatedAt(LocalDateTime.now());

        portfolioHoldingRepository.save(holding);

        order.setStatus(OrderStatus.FILLED);
        order.setUpdatedAt(LocalDateTime.now());

        Order savedOrder = orderRepository.save(order);

        return toResponse(savedOrder);
    }

    @Override
    @Transactional
    public OrderResponse cancelOrder(Long orderId) {

        Order existingOrder = orderRepository
                .findById(orderId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Order not found: " + orderId
                        )
                );

        traderRepository
                .findByIdForUpdate(existingOrder.getTraderId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Trader not found: "
                                        + existingOrder.getTraderId()
                        )
                );

        Order order = orderRepository
                .findByIdForUpdate(orderId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Order not found: " + orderId
                        )
                );

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new BusinessException(
                    "Order cannot be cancelled because current status is: "
                            + order.getStatus()
            );
        }

        if (order.getSide() == OrderSide.SELL) {

            PortfolioHolding holding =
                    portfolioHoldingRepository
                            .findByTraderIdAndStockForUpdate(
                                    order.getTraderId(),
                                    order.getStock()
                            )
                            .orElseThrow(() ->
                                    new BusinessException(
                                            "Portfolio holding not found "
                                                    + "for SELL order"
                                    )
                            );

            if (holding.getReservedQuantity()
                    < order.getQuantity()) {

                throw new BusinessException(
                        "Reserved quantity is insufficient "
                                + "to cancel SELL order"
                );
            }

            holding.setReservedQuantity(
                    holding.getReservedQuantity()
                            - order.getQuantity()
            );

            holding.setUpdatedAt(LocalDateTime.now());

            portfolioHoldingRepository.save(holding);
        }

        order.setStatus(OrderStatus.CANCELLED);
        order.setUpdatedAt(LocalDateTime.now());

        Order savedOrder = orderRepository.save(order);

        return toResponse(savedOrder);
    }

    private OrderResponse toResponse(Order order) {

        return new OrderResponse(
                order.getId(),
                order.getTraderId(),
                order.getStock(),
                order.getSector(),
                order.getQuantity(),
                order.getSide(),
                order.getStatus(),
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }
}