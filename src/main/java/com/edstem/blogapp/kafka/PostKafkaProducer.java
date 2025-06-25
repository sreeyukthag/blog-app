package com.edstem.blogapp.kafka;

import com.edstem.blogapp.dto.PostDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PostKafkaProducer {

    private final KafkaTemplate<String, PostDTO> kafkaTemplate;
    private static final String TOPIC = "post-events";

    public void sendPostCreatedMessage(PostDTO message) {
        kafkaTemplate.send(TOPIC, message);
    }
}
