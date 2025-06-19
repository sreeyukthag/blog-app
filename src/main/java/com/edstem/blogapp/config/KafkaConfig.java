package com.edstem.blogapp.config;

import com.edstem.blogapp.dto.PostDTO;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class KafkaConfig {

    private final KafkaTemplate<String, PostDTO> kafkaTemplate;
    private final Environment environment;

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id}")
    private String groupId;

    @PostConstruct
    public void logConfiguration() {
        log.info("=== KAFKA CONFIGURATION DEBUG ===");
        log.info("Bootstrap Servers (resolved): {}", bootstrapServers);
        log.info("Consumer Group (resolved): {}", groupId);
        log.info("Active Profile: {}", String.join(",", environment.getActiveProfiles()));

        // Debug environment variables
        log.info("Spring Properties:");
        log.info("  spring.kafka.bootstrap-servers: {}", environment.getProperty("spring.kafka.bootstrap-servers"));
        log.info("  spring.kafka.consumer.group-id: {}", environment.getProperty("spring.kafka.consumer.group-id"));

        // Debug environment variables
        log.info("Environment Variables:");
        log.info("  SPRING_KAFKA_BOOTSTRAP_SERVERS: {}", environment.getProperty("SPRING_KAFKA_BOOTSTRAP_SERVERS"));
        log.info("  SPRING_KAFKA_CONSUMER_GROUP_ID: {}", environment.getProperty("SPRING_KAFKA_CONSUMER_GROUP_ID"));
        log.info("  SPRING_PROFILES_ACTIVE: {}", environment.getProperty("SPRING_PROFILES_ACTIVE"));

        log.info("================================");
    }

    @Bean
    @Primary
    public KafkaAdmin kafkaAdmin() {
        log.info("Configuring KafkaAdmin with bootstrap servers: {}", bootstrapServers);
        Map<String, Object> configs = new HashMap<>();
        configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        return new KafkaAdmin(configs);
    }

    @Bean
    public NewTopic userTopic() {
        return TopicBuilder.name("user-topic")
                .partitions(5)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic userDltTopic() {
        return TopicBuilder.name("user-topic-dlt")
                .replicas(1)
                .partitions(5)
                .build();
    }

    @Bean
    public ConsumerFactory<String, PostDTO> consumerFactory() {
        log.info("Creating ConsumerFactory with bootstrap servers: {}", bootstrapServers);
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);

        JsonDeserializer<PostDTO> jsonDeserializer = new JsonDeserializer<>(PostDTO.class);
        jsonDeserializer.addTrustedPackages("*");

        return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), jsonDeserializer);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, PostDTO> kafkaListenerContainerFactory() {
        var factory = new ConcurrentKafkaListenerContainerFactory<String, PostDTO>();
        factory.setConsumerFactory(consumerFactory());
        factory.setConcurrency(3);

        var errorHandler = new DefaultErrorHandler(
                new DeadLetterPublishingRecoverer(kafkaTemplate,
                        (record, e) -> {
                            log.info("Message failed due to {}", e.getMessage());
                            return new TopicPartition("user-topic-dlt", record.partition());
                        }
                ),
                new FixedBackOff(1000L, 2)
        );

        factory.setCommonErrorHandler(errorHandler);
        return factory;
    }
}