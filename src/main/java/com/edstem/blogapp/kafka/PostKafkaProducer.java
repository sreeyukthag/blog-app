package com.edstem.blogapp.kafka;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PostKafkaProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private static final String TOPIC = "post-events";

    public void sendPostCreatedMessage(String message) {
        kafkaTemplate.send(TOPIC, message);
    }
}