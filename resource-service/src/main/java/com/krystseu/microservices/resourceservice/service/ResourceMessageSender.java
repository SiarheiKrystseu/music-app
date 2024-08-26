package com.krystseu.microservices.resourceservice.service;

import com.krystseu.microservices.resourceservice.firebase.FirebaseAuthentication;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Slf4j
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
        String traceId = MDC.get("traceId");
        String authToken = getAuthTokenFromContext();

        Message message = MessageBuilder.withBody(resourceId.getBytes())
                .setHeader("X-Trace-ID", traceId)
                .setHeader("Authorization", "Bearer " + authToken) // Add Authorization header
                .build();

        log.info("Sending message with Resource ID: {}, Trace ID: {}, and Authorization header to Queue: {}", resourceId, traceId, queueName);
        rabbitTemplate.send(queueName, message);
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
}