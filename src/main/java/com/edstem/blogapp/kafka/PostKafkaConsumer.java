package com.edstem.blogapp.kafka;

import com.edstem.blogapp.dto.PostDTO;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
public class PostKafkaConsumer {

    @KafkaListener(topics = "post-events", groupId = "blog-group")
    public void listenToPostEvents(@Payload PostDTO message, Acknowledgment acknowledgment) {
        System.out.println("🎯 Kafka Consumer received message: " + message);

        if (message.getTitle() != null && message.getTitle().contains("fail")) {
            System.err.println("❌ Simulated failure for: " + message);
            throw new RuntimeException("Simulated processing failure");
        }

        acknowledgment.acknowledge();
        System.out.println("✅ Message acknowledged: " + message);
    }

    @KafkaListener(topics = "post-events.DLT", groupId = "blog-dlt-group")
    public void listenToPostDLT(@Payload PostDTO message) {
        System.err.println("🚨 DLT received message: " + message);
    }
}
