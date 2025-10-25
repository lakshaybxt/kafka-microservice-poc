package com.food.order_processing_service.kafka.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.food.order_processing_service.domain.entity.OrderStatus;
import com.food.order_processing_service.kafka.events.OrderEvent;
import com.food.order_processing_service.kafka.events.PaymentEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.food.order_processing_service.kafka.topics.KafkaTopics.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class ListenerService {

    private final ObjectMapper objectMapper;
    private final KafkaTemplate<String, OrderEvent> kafkaTemplate;

    private final Map<String, OrderStatus> orderStatusMap =  new ConcurrentHashMap<>();

    @KafkaListener(topics = ORDER_CREATED, groupId = "${spring.kafka.consumer.group-id}")
    public void onOrderCreated(String message, Acknowledgment ack) {
        OrderEvent orderEvent = objectMapper.convertValue(message, OrderEvent.class);
        String orderId = orderEvent.getOrderId();

        OrderStatus status = orderStatusMap.computeIfAbsent(orderId, id -> new OrderStatus());
        status.setOrderEvent(orderEvent);
        log.info("Order created received for {}", orderId);

        ack.acknowledge();

        tryProcess(orderId);
    }

    @KafkaListener(topics = PAYMENT_COMPLETED, groupId = "${spring.kafka.consumer.group-id}")
    public void onPaymentCompleted(String message, Acknowledgment ack) {
        PaymentEvent paymentEvent = objectMapper.convertValue(message, PaymentEvent.class);
        String orderId = paymentEvent.getOrderId();

        OrderStatus status = orderStatusMap.computeIfAbsent(orderId, id -> new OrderStatus());
        status.setPaymentDone(true);
        log.info("Payment completed for {}", orderId);

        ack.acknowledge();

        tryProcess(orderId);
    }

    private void tryProcess(String orderId) {
        OrderStatus status = orderStatusMap.get(orderId);
        if (status != null && status.isPaymentDone() && status.getOrderEvent() != null) {
            log.info("Processing order {}", orderId);
            kafkaTemplate.send(ORDER_PROCESSED, status.getOrderEvent());
            orderStatusMap.remove(orderId);
        }
    }
}
