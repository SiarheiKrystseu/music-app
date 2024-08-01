package com.krystseu.microservices.resourceprocessor.config;

import org.springframework.amqp.core.Queue;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

    @Value("${resource.rabbitmq.queue}")
    private String resourceQueueName;

    @Value("${resource.ack.queue}")
    private String resourceAckQueueName;

    @Bean
    Queue resourceQueue() {
        return new Queue(resourceQueueName, false);
    }

    @Bean
    Queue resourceAckQueue() {
        return new Queue(resourceAckQueueName, false);
    }
}
