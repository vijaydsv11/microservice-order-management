package com.microservices.orderservice.service;

import com.microservices.orderservice.client.UserServiceClient;
import com.microservices.orderservice.dto.OrderDTO;
import com.microservices.orderservice.entity.Order;
import com.microservices.orderservice.event.OrderCreatedEvent;
import com.microservices.orderservice.producer.OrderCreatedEventProducer;
import com.microservices.orderservice.repository.OrderRepository;
import feign.FeignException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Order service with functional Stream API usage
 */
@Service
@AllArgsConstructor
@Slf4j
public class OrderService {

        private static final String USER_SERVICE_RESILIENCE = "userService";

        private OrderRepository orderRepository;
        private UserServiceClient userServiceClient;
        private OrderCreatedEventProducer eventProducer;

        public OrderDTO createOrder(OrderDTO orderDTO) {
                // Validate user exists via Feign client
                UserServiceClient.UserResponse userResponse = getUserWithResilience(orderDTO.userId());

                Order order = new Order();
                order.setUserId(orderDTO.userId());
                order.setProductName(orderDTO.productName());
                order.setQuantity(orderDTO.quantity());
                order.setPrice(orderDTO.price());
                order.setTotalAmount(orderDTO.totalAmount());
                order.setStatus(Order.OrderStatus.PENDING);

                Order savedOrder = orderRepository.save(order);

                // Publish event to Kafka with timestamp using record constructor
                OrderCreatedEvent event = new OrderCreatedEvent(
                                savedOrder.getId(),
                                savedOrder.getUserId(),
                                savedOrder.getProductName(),
                                savedOrder.getQuantity(),
                                savedOrder.getTotalAmount(),
                                userResponse.email,
                                System.currentTimeMillis());

                eventProducer.sendOrderCreatedEvent(event);

                return convertToDTO(savedOrder, userResponse);
        }

        public OrderDTO getOrderById(Long id) {
                Order order = orderRepository.findById(id)
                                .orElseThrow(() -> new RuntimeException("Order not found"));

                UserServiceClient.UserResponse userResponse = getUserWithResilience(order.getUserId());

                return convertToDTO(order, userResponse);
        }

        public List<OrderDTO> getOrdersByUserId(Long userId) {
                List<Order> orders = orderRepository.findByUserId(userId);
                UserServiceClient.UserResponse userResponse = getUserWithResilience(userId);

                // Use Stream API for functional programming
                return orders.stream()
                                .map(order -> convertToDTO(order, userResponse))
                                .collect(Collectors.toList());
        }

        public List<OrderDTO> getAllOrders() {
                // Functional stream processing with parallel support
                return orderRepository.findAll()
                                .parallelStream()
                                .map(order -> {
                                        UserServiceClient.UserResponse userResponse = getUserWithResilience(
                                                        order.getUserId());
                                        return convertToDTO(order, userResponse);
                                })
                                .collect(Collectors.toList());
        }

        public OrderDTO updateOrderStatus(Long id, String status) {
                Order order = orderRepository.findById(id)
                                .orElseThrow(() -> new RuntimeException("Order not found"));

                // Validate status using Stream
                boolean validStatus = java.util.Arrays.stream(Order.OrderStatus.values())
                                .anyMatch(s -> s.toString().equals(status.toUpperCase()));

                if (!validStatus) {
                        throw new RuntimeException("Invalid order status: " + status);
                }

                order.setStatus(Order.OrderStatus.valueOf(status.toUpperCase()));
                order.setUpdatedAt(LocalDateTime.now());

                Order updatedOrder = orderRepository.save(order);

                UserServiceClient.UserResponse userResponse = getUserWithResilience(updatedOrder.getUserId());

                return convertToDTO(updatedOrder, userResponse);
        }

        public void deleteOrder(Long id) {
                orderRepository.deleteById(id);
        }

        /**
         * Saga step 3 (success path) — confirms the order after inventory-service
         * successfully reserved stock. Called by the inventoryReservedConsumer bean.
         */
        @Transactional
        public void confirmOrder(Long orderId) {
                orderRepository.findById(orderId).ifPresentOrElse(order -> {
                        order.setStatus(Order.OrderStatus.CONFIRMED);
                        order.setUpdatedAt(LocalDateTime.now());
                        orderRepository.save(order);
                        log.info("Saga: order {} confirmed — inventory reserved successfully", orderId);
                }, () -> log.warn("Saga: confirmOrder called for unknown orderId={}", orderId));
        }

        /**
         * Saga step 3 (compensation path) — cancels the order when inventory
         * reservation failed. Called by the inventoryFailedConsumer bean.
         */
        @Transactional
        public void cancelOrder(Long orderId, String reason) {
                orderRepository.findById(orderId).ifPresentOrElse(order -> {
                        order.setStatus(Order.OrderStatus.CANCELLED);
                        order.setUpdatedAt(LocalDateTime.now());
                        orderRepository.save(order);
                        log.warn("Saga: order {} cancelled (compensation). Reason: {}", orderId, reason);
                }, () -> log.warn("Saga: cancelOrder called for unknown orderId={}", orderId));
        }

        @Retry(name = USER_SERVICE_RESILIENCE, fallbackMethod = "getUserWithResilienceFallback")
        @CircuitBreaker(name = USER_SERVICE_RESILIENCE, fallbackMethod = "getUserWithResilienceFallback")
        public UserServiceClient.UserResponse getUserWithResilience(Long userId) {
                UserServiceClient.UserResponse userResponse = userServiceClient.getUserById(userId).getBody();
                if (userResponse == null) {
                        throw new RuntimeException("User not found");
                }
                return userResponse;
        }

        public UserServiceClient.UserResponse getUserWithResilienceFallback(Long userId, Throwable throwable) {
                if (throwable instanceof FeignException.NotFound) {
                        throw new RuntimeException("User not found");
                }

                log.warn("User service unavailable for userId {}. Returning fallback user. Cause: {}",
                                userId, throwable.getMessage());

                UserServiceClient.UserResponse fallback = new UserServiceClient.UserResponse();
                fallback.id = userId;
                fallback.firstName = "User";
                fallback.lastName = "Unavailable";
                fallback.email = "unknown@unavailable.local";
                return fallback;
        }

        private OrderDTO convertToDTO(Order order, UserServiceClient.UserResponse userResponse) {
                String userName = userResponse != null
                                ? userResponse.firstName + " " + userResponse.lastName
                                : "Unknown";

                String userEmail = userResponse != null ? userResponse.email : null;

                return new OrderDTO(
                                order.getId(),
                                order.getUserId(),
                                order.getProductName(),
                                order.getQuantity(),
                                order.getPrice(),
                                order.getTotalAmount(),
                                order.getStatus().toString(),
                                userName,
                                userEmail);
        }

}
