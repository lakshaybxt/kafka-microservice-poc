package com.food.food_service.kafka.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.food.food_service.kafka.events.OrderEvent;
import com.food.food_service.kafka.events.OrderItemPreparedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

import static com.food.food_service.kafka.topics.KafkaTopics.ORDER_PREPARED;
import static com.food.food_service.kafka.topics.KafkaTopics.ORDER_PROCESSED;

@Service
@Slf4j
@RequiredArgsConstructor
public class ListenerService {

    private final ObjectMapper objectMapper;
    private final KafkaTemplate<String, OrderItemPreparedEvent> kafkaTemplate;

    @KafkaListener(topics = ORDER_PROCESSED, groupId = "${spring.kafka.consumer.group-id}")
    public void onOrderProcessed(String message, Acknowledgment ack) {
        try {
            OrderEvent orderEvent = objectMapper.readValue(message, OrderEvent.class);
            String userId = orderEvent.getUserId();
            String orderId = orderEvent.getOrderId();

            log.info("Order processed received for {}", orderId);
            new Thread(() -> {
                try {
                    Thread.sleep(1000); // 1 min delay for order preparation
                    ack.acknowledge(); // Ack after 1 minute
                    log.info("Order has bee prepared for orderId: {}, userId: {}", orderId, userId);

                    OrderItemPreparedEvent orderItemPreparedEvent = OrderItemPreparedEvent.builder()
                            .orderId(orderId)
                            .userId(userId)
                            .itemType(orderEvent.getFood().getType())
                            .quantity(orderEvent.getFood().getQuantity())
                            .address(orderEvent.getAddress())
                            .completedTime(LocalDateTime.now())
                            .build();
                    kafkaTemplate.send(ORDER_PREPARED, orderItemPreparedEvent);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.error("Ack delay interrupted", e);
                }
            }).start();
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

}
