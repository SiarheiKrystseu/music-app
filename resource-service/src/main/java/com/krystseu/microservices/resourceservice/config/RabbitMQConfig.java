package com.krystseu.microservices.resourceservice.config;

import com.krystseu.microservices.resourceservice.firebase.FirebaseAuthentication;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;


@Configuration
@Slf4j
public class RabbitMQConfig {

    @Value("${resource.rabbitmq.queue}")
    private String resourceQueueName;

    @Value("${resource.ack.queue}")
    private String resourceAckQueueName;

    @Value("${spring.rabbitmq.host}")
    private String rabbitMqHost;

    @Value("${spring.rabbitmq.port}")
    private int rabbitMqPort;

    @Value("${spring.rabbitmq.username}")
    private String rabbitMqUsername;

    @Value("${spring.rabbitmq.password}")
    private String rabbitMqPassword;

    @Bean
    public Queue processQueue() {
        return new Queue(resourceQueueName, false);
    }

    @Bean
    public Queue ackQueue() {
        return new Queue(resourceAckQueueName, false);
    }

    @Bean
    public DirectExchange exchange() {
        return new DirectExchange("resource.exchange");
    }

    @Bean
    public Binding bindingProcessQueue(Queue processQueue, DirectExchange exchange) {
        return BindingBuilder.bind(processQueue).to(exchange).with("process.routing.key");
    }

    @Bean
    public Binding bindingAckQueue(Queue ackQueue, DirectExchange exchange) {
        return BindingBuilder.bind(ackQueue).to(exchange).with("ack.routing.key");
    }

    @Bean
    public ConnectionFactory connectionFactory() {
        CachingConnectionFactory connectionFactory = new CachingConnectionFactory();
        connectionFactory.setHost(rabbitMqHost);
        connectionFactory.setPort(rabbitMqPort);
        connectionFactory.setUsername(rabbitMqUsername);
        connectionFactory.setPassword(rabbitMqPassword);
        return connectionFactory;
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setReplyTimeout(5000);

        // Adding a BeforePublishPostProcessor to include the trace ID and Authorization header in message headers
        rabbitTemplate.setBeforePublishPostProcessors(message -> {
            // Retrieve the trace ID from MDC
            String traceId = MDC.get("traceId");
            if (traceId != null) {
                message.getMessageProperties().setHeader("X-Trace-ID", traceId);
            }

            // Retrieve the Authorization header from the security context
            String authToken = getAuthTokenFromContext();
            if (!authToken.isEmpty()) {
                message.getMessageProperties().setHeader("Authorization", "Bearer " + authToken);
            }

            log.info("Sending message with Trace ID: {} and Authorization: {} to Exchange: {} with Routing Key: {}",
                    traceId, authToken, message.getMessageProperties().getReceivedExchange(),
                    message.getMessageProperties().getReceivedRoutingKey());

            return message;
        });

        return rabbitTemplate;
    }

    private String getAuthTokenFromContext() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof FirebaseAuthentication firebaseAuth) {
            log.info("Retrieving auth token: {}", firebaseAuth.getRawToken());
            return firebaseAuth.getRawToken();
        }
        log.warn("Authorization token not found in the security context.");
        return "";
    }

    @Bean
    public String resourceQueueName() {
        return resourceQueueName;
    }

    @Bean
    public String ackQueueName() {
        return resourceAckQueueName;
    }
}





