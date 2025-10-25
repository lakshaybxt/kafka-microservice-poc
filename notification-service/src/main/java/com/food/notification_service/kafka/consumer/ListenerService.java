package com.food.notification_service.kafka.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.food.notification_service.kafka.events.OrderReadyForDeliveryEvent;
import com.food.notification_service.kafka.events.PaymentEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

import static com.food.notification_service.kafka.topics.KafkaTopics.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class ListenerService {

    private final ObjectMapper objectMapper;

    @KafkaListener(topics = PAYMENT_COMPLETED, groupId = "${spring.kafka.consumer.group-id}")
    public void handlePaymentCompleted(String message, Acknowledgment ack) throws JsonProcessingException {
        PaymentEvent event = objectMapper.readValue(message, PaymentEvent.class);
        ack.acknowledge();
        log.info("Notification: Payment COMPLETED for user {} on order {}. Amount: {}",
                event.getUserId(), event.getOrderId(), event.getAmount());
    }

    @KafkaListener(topics = ORDER_READY_FOR_DELIVER, groupId = "${spring.kafka.consumer.group-id}")
    public void handleOrderReadyForDelivery(String message, Acknowledgment ack) throws JsonProcessingException {
        OrderReadyForDeliveryEvent event = objectMapper.readValue(message, OrderReadyForDeliveryEvent.class);
        ack.acknowledge();
        log.info("Notification: Order {} is READY FOR DELIVERY for user {}. Address: {}. Item: {}. Message: {}",
                event.getOrderId(), event.getUserId(), event.getAddress(), event.getItem(), event.getMessage());
    }

    @KafkaListener(topics = ORDER_DELIVERED, groupId = "${spring.kafka.consumer.group-id}")
    public void handleOrderDelivered(String message, Acknowledgment ack) throws JsonProcessingException {
        OrderReadyForDeliveryEvent event = objectMapper.readValue(message, OrderReadyForDeliveryEvent.class);
        ack.acknowledge();
        log.info("Notification: Order {} is DELIVERED for user {}. Address: {}. Item: {}. Message: {}",
                event.getOrderId(), event.getUserId(), event.getAddress(), event.getItem(), event.getMessage());
    }
}
