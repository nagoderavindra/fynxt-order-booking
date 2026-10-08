package com.fynxt.service;

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
import com.fynxt.service.impl.OrderServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private TraderRepository traderRepository;

    @Mock
    private PortfolioHoldingRepository portfolioHoldingRepository;

    @InjectMocks
    private OrderServiceImpl orderService;

    private Trader trader;

    @BeforeEach
    void setUp() {

        trader = new Trader(
                "T001",
                "Demo Trader",
                LocalDateTime.now()
        );
    }

    @Test
    void shouldPlaceBuyOrderSuccessfully() {

        PlaceOrderRequest request = new PlaceOrderRequest();

        request.setTraderId("T001");
        request.setStock("AAPL");
        request.setSector("TECH");
        request.setQuantity(50);
        request.setSide(OrderSide.BUY);

        when(traderRepository.findByIdForUpdate("T001"))
                .thenReturn(Optional.of(trader));

        when(orderRepository.countByTraderIdAndStatus(
                "T001",
                OrderStatus.PENDING
        )).thenReturn(0L);

        Order savedOrder = createOrder(
                1L,
                "T001",
                "AAPL",
                "TECH",
                50,
                OrderSide.BUY,
                OrderStatus.PENDING
        );

        when(orderRepository.save(any(Order.class)))
                .thenReturn(savedOrder);

        OrderResponse response =
                orderService.placeOrder(request);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("T001", response.getTraderId());
        assertEquals("AAPL", response.getStock());
        assertEquals(50, response.getQuantity());
        assertEquals(OrderSide.BUY, response.getSide());
        assertEquals(OrderStatus.PENDING, response.getStatus());

        verify(orderRepository)
                .save(any(Order.class));
    }

    @Test
    void shouldPlaceSellOrderWhenEnoughSharesAreAvailable() {

        PlaceOrderRequest request = new PlaceOrderRequest();

        request.setTraderId("T001");
        request.setStock("AAPL");
        request.setSector("TECH");
        request.setQuantity(50);
        request.setSide(OrderSide.SELL);

        PortfolioHolding holding = createHolding(
                "T001",
                "AAPL",
                "TECH",
                100,
                0
        );

        when(traderRepository.findByIdForUpdate("T001"))
                .thenReturn(Optional.of(trader));

        when(orderRepository.countByTraderIdAndStatus(
                "T001",
                OrderStatus.PENDING
        )).thenReturn(0L);

        when(portfolioHoldingRepository
                .findByTraderIdAndStockForUpdate(
                        "T001",
                        "AAPL"
                ))
                .thenReturn(Optional.of(holding));

        Order savedOrder = createOrder(
                2L,
                "T001",
                "AAPL",
                "TECH",
                50,
                OrderSide.SELL,
                OrderStatus.PENDING
        );

        when(orderRepository.save(any(Order.class)))
                .thenReturn(savedOrder);

        OrderResponse response =
                orderService.placeOrder(request);

        assertNotNull(response);
        assertEquals(OrderSide.SELL, response.getSide());
        assertEquals(OrderStatus.PENDING, response.getStatus());

        assertEquals(
                50,
                holding.getReservedQuantity()
        );

        verify(portfolioHoldingRepository)
                .save(holding);

        verify(orderRepository)
                .save(any(Order.class));
    }

    @Test
    void shouldRejectSellOrderWhenInsufficientShares() {

        PlaceOrderRequest request = new PlaceOrderRequest();

        request.setTraderId("T001");
        request.setStock("AAPL");
        request.setSector("TECH");
        request.setQuantity(150);
        request.setSide(OrderSide.SELL);

        PortfolioHolding holding = createHolding(
                "T001",
                "AAPL",
                "TECH",
                100,
                0
        );

        when(traderRepository.findByIdForUpdate("T001"))
                .thenReturn(Optional.of(trader));

        when(orderRepository.countByTraderIdAndStatus(
                "T001",
                OrderStatus.PENDING
        )).thenReturn(0L);

        when(portfolioHoldingRepository
                .findByTraderIdAndStockForUpdate(
                        "T001",
                        "AAPL"
                ))
                .thenReturn(Optional.of(holding));

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> orderService.placeOrder(request)
                );

        assertTrue(
                exception.getMessage()
                        .contains("Insufficient shares")
        );

        verify(orderRepository, never())
                .save(any(Order.class));
    }

    @Test
    void shouldRejectOrderWhenPendingOrderLimitReached() {

        PlaceOrderRequest request = new PlaceOrderRequest();

        request.setTraderId("T001");
        request.setStock("AAPL");
        request.setSector("TECH");
        request.setQuantity(50);
        request.setSide(OrderSide.BUY);

        when(traderRepository.findByIdForUpdate("T001"))
                .thenReturn(Optional.of(trader));

        when(orderRepository.countByTraderIdAndStatus(
                "T001",
                OrderStatus.PENDING
        )).thenReturn(3L);

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> orderService.placeOrder(request)
                );

        assertEquals(
                "Trader cannot have more than 3 pending orders",
                exception.getMessage()
        );

        verify(orderRepository, never())
                .save(any(Order.class));
    }

    @Test
    void shouldThrowExceptionWhenTraderNotFound() {

        PlaceOrderRequest request = new PlaceOrderRequest();

        request.setTraderId("T999");
        request.setStock("AAPL");
        request.setSector("TECH");
        request.setQuantity(50);
        request.setSide(OrderSide.BUY);

        when(traderRepository.findByIdForUpdate("T999"))
                .thenReturn(Optional.empty());

        ResourceNotFoundException exception =
                assertThrows(
                        ResourceNotFoundException.class,
                        () -> orderService.placeOrder(request)
                );

        assertEquals(
                "Trader not found: T999",
                exception.getMessage()
        );

        verify(orderRepository, never())
                .save(any(Order.class));
    }

    @Test
    void shouldFillPendingBuyOrderSuccessfully() {

        Order order = createOrder(
                1L,
                "T001",
                "AAPL",
                "TECH",
                50,
                OrderSide.BUY,
                OrderStatus.PENDING
        );

        when(orderRepository.findById(1L))
                .thenReturn(Optional.of(order));

        when(traderRepository.findByIdForUpdate("T001"))
                .thenReturn(Optional.of(trader));

        when(orderRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(order));

        when(portfolioHoldingRepository
                .findByTraderIdAndStockForUpdate(
                        "T001",
                        "AAPL"
                ))
                .thenReturn(Optional.empty());

        when(portfolioHoldingRepository.save(
                any(PortfolioHolding.class)
        )).thenAnswer(invocation -> invocation.getArgument(0));

        when(orderRepository.save(any(Order.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        OrderResponse response =
                orderService.fillOrder(1L);

        assertEquals(
                OrderStatus.FILLED,
                response.getStatus()
        );

        verify(portfolioHoldingRepository)
                .save(any(PortfolioHolding.class));

        verify(orderRepository)
                .save(order);
    }

    @Test
    void shouldRejectFillingNonPendingOrder() {

        Order order = createOrder(
                1L,
                "T001",
                "AAPL",
                "TECH",
                50,
                OrderSide.BUY,
                OrderStatus.FILLED
        );

        when(orderRepository.findById(1L))
                .thenReturn(Optional.of(order));

        when(traderRepository.findByIdForUpdate("T001"))
                .thenReturn(Optional.of(trader));

        when(orderRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(order));

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> orderService.fillOrder(1L)
                );

        assertTrue(
                exception.getMessage()
                        .contains("Order cannot be filled")
        );
    }

    @Test
    void shouldCancelPendingBuyOrderSuccessfully() {

        Order order = createOrder(
                1L,
                "T001",
                "AAPL",
                "TECH",
                50,
                OrderSide.BUY,
                OrderStatus.PENDING
        );

        when(orderRepository.findById(1L))
                .thenReturn(Optional.of(order));

        when(traderRepository.findByIdForUpdate("T001"))
                .thenReturn(Optional.of(trader));

        when(orderRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(order));

        when(orderRepository.save(any(Order.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        OrderResponse response =
                orderService.cancelOrder(1L);

        assertEquals(
                OrderStatus.CANCELLED,
                response.getStatus()
        );

        verify(orderRepository)
                .save(order);
    }

    @Test
    void shouldRejectCancellingNonPendingOrder() {

        Order order = createOrder(
                1L,
                "T001",
                "AAPL",
                "TECH",
                50,
                OrderSide.BUY,
                OrderStatus.CANCELLED
        );

        when(orderRepository.findById(1L))
                .thenReturn(Optional.of(order));

        when(traderRepository.findByIdForUpdate("T001"))
                .thenReturn(Optional.of(trader));

        when(orderRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(order));

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> orderService.cancelOrder(1L)
                );

        assertTrue(
                exception.getMessage()
                        .contains("Order cannot be cancelled")
        );
    }

    private Order createOrder(
            Long id,
            String traderId,
            String stock,
            String sector,
            Integer quantity,
            OrderSide side,
            OrderStatus status) {

        LocalDateTime now = LocalDateTime.now();

        Order order = new Order();

        order.setId(id);
        order.setTraderId(traderId);
        order.setStock(stock);
        order.setSector(sector);
        order.setQuantity(quantity);
        order.setSide(side);
        order.setStatus(status);
        order.setCreatedAt(now);
        order.setUpdatedAt(now);

        return order;
    }

    private PortfolioHolding createHolding(
            String traderId,
            String stock,
            String sector,
            Integer quantity,
            Integer reservedQuantity) {

        LocalDateTime now = LocalDateTime.now();

        PortfolioHolding holding =
                new PortfolioHolding();

        holding.setTraderId(traderId);
        holding.setStock(stock);
        holding.setSector(sector);
        holding.setQuantity(quantity);
        holding.setReservedQuantity(reservedQuantity);
        holding.setCreatedAt(now);
        holding.setUpdatedAt(now);

        return holding;
    }
}