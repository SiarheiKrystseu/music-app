package com.krystseu.microservices.resourceprocessor.service.config;

import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import jakarta.annotation.PostConstruct;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;

import java.time.Duration;

@TestConfiguration
public class TestRabbitMqConfig {

    @Value("${resource.rabbitmq.queue}")
    private String queueName;

    @Value("${resource.ack.queue}")
    private String resourceAckQueueName;

    @Container
    public RabbitMQContainer rabbitMqContainer = new RabbitMQContainer()
            .waitingFor(Wait.forListeningPort())
            .withStartupTimeout(Duration.ofMinutes(1))
            .withExposedPorts(5672, 15672);

    @PostConstruct
    public void startContainer() {
        rabbitMqContainer.start();
    }

    @Bean
    Queue queue() {
        return new Queue(queueName, false);
    }

    @Bean
    Queue resourceAckQueue() {
        return new Queue(resourceAckQueueName, false);
    }

    @Bean
    public ConnectionFactory connectionFactory() {
        CachingConnectionFactory connectionFactory = new CachingConnectionFactory();
        connectionFactory.setHost(rabbitMqContainer.getContainerIpAddress());
        connectionFactory.setPort(rabbitMqContainer.getMappedPort(5672));
        return connectionFactory;
    }
}