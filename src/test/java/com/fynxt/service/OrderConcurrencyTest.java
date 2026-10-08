package com.fynxt.service;

import com.fynxt.dto.OrderResponse;
import com.fynxt.dto.PlaceOrderRequest;
import com.fynxt.entity.OrderStatus;
import com.fynxt.entity.OrderSide;
import com.fynxt.exception.BusinessException;
import com.fynxt.repository.OrderRepository;
import com.fynxt.repository.TraderRepository;
import com.fynxt.service.impl.OrderServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class OrderConcurrencyTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private TraderRepository traderRepository;

    @BeforeEach
    void cleanUp() {

        orderRepository.deleteAll();

        assertTrue(
                traderRepository.findById("T001").isPresent(),
                "T001 trader must exist"
        );
    }

    @Test
    void shouldAllowMaximumThreePendingOrdersDuringConcurrentRequests()
            throws Exception {

        int numberOfRequests = 10;

        ExecutorService executorService =
                Executors.newFixedThreadPool(numberOfRequests);

        CountDownLatch startLatch =
                new CountDownLatch(1);

        List<Future<Boolean>> results =
                new ArrayList<>();

        for (int i = 0; i < numberOfRequests; i++) {

            int requestNumber = i;

            results.add(
                    executorService.submit(() -> {

                        startLatch.await();

                        PlaceOrderRequest request =
                                new PlaceOrderRequest();

                        request.setTraderId("T001");
                        request.setStock("AAPL");
                        request.setSector("TECH");
                        request.setQuantity(10);
                        request.setSide(OrderSide.BUY);

                        try {

                            OrderResponse response =
                                    orderService.placeOrder(request);

                            return response.getStatus()
                                    == OrderStatus.PENDING;

                        } catch (BusinessException ex) {

                            return false;
                        }
                    })
            );
        }

        startLatch.countDown();

        int successfulOrders = 0;

        for (Future<Boolean> result : results) {

            if (result.get()) {
                successfulOrders++;
            }
        }

        executorService.shutdown();

        assertTrue(
                executorService.awaitTermination(
                        10,
                        TimeUnit.SECONDS
                )
        );

        long pendingOrders =
                orderRepository.countByTraderIdAndStatus(
                        "T001",
                        OrderStatus.PENDING
                );

        assertEquals(
                3,
                successfulOrders,
                "Only 3 concurrent orders should be accepted"
        );

        assertEquals(
                3,
                pendingOrders,
                "Database should contain only 3 pending orders"
        );
    }
}
