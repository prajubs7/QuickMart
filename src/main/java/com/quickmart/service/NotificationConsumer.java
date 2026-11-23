package com.quickmart.service;

import com.quickmart.dto.OrderPlacedEvent;
import com.quickmart.entity.Notification;
import com.quickmart.repository.NotificationRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Notification service: tells the customer their order is confirmed.
 *
 * BROKEN ON PURPOSE (scenario 11): some customers get the same SMS 2, 3, 4 times. Find out why.
 */
@Slf4j
@Service
public class NotificationConsumer {

    private final NotificationRepository notificationRepository;
    private final double pushFailureRate;

    public NotificationConsumer(NotificationRepository notificationRepository,
                                @Value("${quickmart.notification.push-failure-rate}") double pushFailureRate) {
        this.notificationRepository = notificationRepository;
        this.pushFailureRate = pushFailureRate;
    }

    // groupId: all instances of the notification service share one group, so each event is
    // handled by ONE of them. Analytics would use its own group and get its own copy of every event.
    @KafkaListener(topics = OrderPlacedEvent.TOPIC, groupId = "notification-service")
    public void onOrderPlaced(OrderPlacedEvent event) {
        log.info("Notification: got eventId={} orderId={}", event.eventId(), event.orderId());

        // 1. SMS. The saved row stands for "the customer's phone buzzed".
        //    No @Transactional here, so save() commits immediately.
        notificationRepository.save(new Notification(event.orderId(), "SMS",
                "Your QuickMart order #" + event.orderId() + " is confirmed. Total: " + event.totalAmount()));

        // 2. Push notification through a flaky provider.
        if (ThreadLocalRandom.current().nextDouble() < pushFailureRate) {
            log.warn("Notification: push for orderId={} FAILED", event.orderId());
            throw new IllegalStateException("Push provider timed out");
        }
        notificationRepository.save(new Notification(event.orderId(), "PUSH",
                "Order #" + event.orderId() + " confirmed"));

        log.info("Notification: done for orderId={}", event.orderId());
        // Returning normally tells Spring Kafka to commit the offset: this event won't be delivered again.
    }
}
