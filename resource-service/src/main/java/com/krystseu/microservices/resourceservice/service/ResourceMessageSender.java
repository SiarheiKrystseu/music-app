package com.krystseu.microservices.resourceservice.service;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ResourceMessageSender {

    private final RabbitTemplate rabbitTemplate;
    private final String queueName;

    public ResourceMessageSender(RabbitTemplate rabbitTemplate,
                                 @Value("${resource.rabbitmq.queue}") String queueName) {
        this.rabbitTemplate = rabbitTemplate;
        this.queueName = queueName;
    }

    public void sendResourceMessage(String resourceId) {

        String traceId = MDC.get("traceId");
        Message message = MessageBuilder.withBody(resourceId.getBytes())
                .setHeader("X-Trace-ID", traceId)
                .build();
        log.info("Sending message with Resource ID: {} and Trace ID: {} to Queue: {}", resourceId, traceId, queueName);
        rabbitTemplate.send(queueName, message);
    }
}