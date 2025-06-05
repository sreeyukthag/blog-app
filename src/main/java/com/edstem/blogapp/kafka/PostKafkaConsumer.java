package com.edstem.blogapp.kafka;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
public class PostKafkaConsumer {

    // Normal Kafka Topic Listener with retries and DLT handled by error handler
    @KafkaListener(topics = "post-events", groupId = "blog-group")
    public void listenToPostEvents(@Payload String message, Acknowledgment acknowledgment) {
        System.out.println(">>>>>>>>> Kafka Consumer received message: [" + message + "]");

        // Simulating failure
        if (message.contains("fail")) {
            System.out.println(">>>>>>>>> Simulating failure for message: [" + message + "]");
            throw new RuntimeException("Simulated processing failure");
        }

        // Acknowledge message manually
        acknowledgment.acknowledge();
    }

    // DLT listener - take failed messages
    @KafkaListener(topics = "post-events.DLT", groupId = "blog-dlt-group")
    public void listenToPostDLT(@Payload String message) {
        System.err.println(">>>>>>>>>> DLT Consumer received failed message: [" + message + "]");
    }
}
