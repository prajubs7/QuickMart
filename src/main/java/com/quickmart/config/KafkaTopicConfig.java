package com.quickmart.config;

import com.quickmart.dto.OrderPlacedEvent;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    // Spring's KafkaAdmin creates this topic at startup if it doesn't exist.
    // 3 partitions = up to 3 consumers in one group can work in parallel.
    // 1 replica because local Kafka has one broker (prod: 3, so one dead broker loses nothing).
    @Bean
    public NewTopic orderPlacedTopic() {
        return TopicBuilder.name(OrderPlacedEvent.TOPIC).partitions(3).replicas(1).build();
    }
}
