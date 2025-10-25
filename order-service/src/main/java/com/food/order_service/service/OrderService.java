package com.food.order_service.service;

import com.food.order_service.dto.OrderDto;
import com.food.order_service.kafka.events.OrderEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;

import java.time.LocalDateTime;

import static com.food.order_service.kafka.topics.KafkaTopics.ORDER_CREATED;

@Service
@Slf4j
@RequiredArgsConstructor
public class OrderService {

    private final KafkaTemplate<String, OrderEvent> kafkaTemplate;

    public OrderEvent createOrder(OrderDto orderDto){

        long totalAmount = orderDto.getFood().getQuantity() * orderDto.getFood().getPrice();
//        String key = orderDto.getFood().getName();

        OrderEvent orderEvent = OrderEvent.builder()
                .orderId(orderDto.getOrderId())
                .userId(orderDto.getUserId())
                .address(orderDto.getAddress())
                .food(orderDto.getFood())
                .totalAmount(totalAmount)
                .createTime(LocalDateTime.now())
                .build();
        try {
            log.info("Sending Order to Kafka: {}", orderEvent);
            kafkaTemplate.send(ORDER_CREATED, orderEvent);
            return orderEvent;
        } catch (Exception e) {
            log.error("Failed to send message to Kafka", e);
            throw new RuntimeException("Failed to send message to Kafka", e);
        }

    }
}
