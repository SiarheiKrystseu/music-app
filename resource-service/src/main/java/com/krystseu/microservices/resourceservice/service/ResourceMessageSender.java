package com.krystseu.microservices.resourceservice.service;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ResourceMessageSender {

    private final RabbitTemplate rabbitTemplate;
    private final String queueName;

    public ResourceMessageSender(RabbitTemplate rabbitTemplate,
                                 @Value("${resource.rabbitmq.queue}") String queueName) {
        this.rabbitTemplate = rabbitTemplate;
        this.queueName = queueName;
    }

    public void sendResourceMessage(String resourceId) {
        rabbitTemplate.convertAndSend(queueName, resourceId);
    }
}