package com.krystseu.microservices.resourceprocessor.service.impl;

import com.krystseu.microservices.resourceprocessor.exception.FileParsingException;
import com.krystseu.microservices.resourceprocessor.service.ResourceProcessor;
import com.krystseu.microservices.resourceprocessor.service.ResourceServiceClient;
import com.krystseu.microservices.resourceprocessor.service.SongMetadataSavedEvent;
import com.krystseu.microservices.resourceprocessor.service.SongServiceClient;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.mp3.Mp3Parser;
import org.apache.tika.sax.BodyContentHandler;
import org.slf4j.MDC;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import java.nio.charset.StandardCharsets;

@Service
@Slf4j
public class ResourceProcessorImpl implements ResourceProcessor {

    private final ResourceServiceClient resourceServiceClient;
    private final SongServiceClient songServiceClient;
    private final RabbitTemplate rabbitTemplate;

    @Value("${resource.ack.queue}")
    private String resourceAckQueue;

    @Autowired
    public ResourceProcessorImpl(ResourceServiceClient resourceServiceClient, SongServiceClient songServiceClient, RabbitTemplate rabbitTemplate) {
        this.resourceServiceClient = resourceServiceClient;
        this.songServiceClient = songServiceClient;
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    @RabbitListener(queues = "${resource.rabbitmq.queue}")
    @Retryable(value = {WebClientResponseException.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000))
    public void processResourceMessage(Message message) {
        MessageProperties messageProperties = message.getMessageProperties();
        String resourceId = new String(message.getBody(), StandardCharsets.UTF_8);
        String correlationId = messageProperties.getCorrelationId();
        String traceId = (String) messageProperties.getHeader("X-Trace-ID");

        // Set traceId in MDC
        MDC.put("traceId", traceId);
        log.info("Received message with resource ID: {} and correlation ID: {}", resourceId, correlationId);

        try {
            // Process resource data
            processResource(resourceId);
            sendAcknowledgment(resourceId, correlationId, traceId);
            log.info("Sent acknowledgment for resource ID: {} with correlation ID: {}", resourceId, correlationId);
        } catch (Exception e) {
            log.error("Error processing resource ID: {}", resourceId, e);
            throw e;
        } finally {
            MDC.clear(); // Clear MDC after processing
        }
    }

    private void processResource(String resourceId) {
        // Get and process resource data
        byte[] resourceData = resourceServiceClient.getResourceData(resourceId);
        Metadata metadata = extractMetadata(resourceData);
        songServiceClient.saveMetadata(metadata, Long.valueOf(resourceId));
        log.info("Successfully processed resource with ID: {}", resourceId);
    }

    private void sendAcknowledgment(String resourceId, String correlationId, String traceId) {
        MessageProperties ackMessageProperties = new MessageProperties();
        ackMessageProperties.setCorrelationId(correlationId);
        ackMessageProperties.setHeader("X-Trace-ID", traceId);  // Include traceId in acknowledgment

        Message ackMessage = new Message(resourceId.getBytes(StandardCharsets.UTF_8), ackMessageProperties);
        rabbitTemplate.send(resourceAckQueue, ackMessage);
    }

    @Override
    public Metadata extractMetadata(byte[] audioData) {
        try (InputStream input = new ByteArrayInputStream(audioData)) {
            BodyContentHandler handler = new BodyContentHandler();
            Metadata metadata = new Metadata();
            new Mp3Parser().parse(input, handler, metadata, new ParseContext());
            return metadata;
        } catch (IOException | TikaException | SAXException e) {
            throw new FileParsingException("Error while parsing the audio data", e);
        }
    }

    @Recover
    public void recover(WebClientResponseException e, String resourceId) {
        log.error("Failed to retrieve resource with ID: " + resourceId, e);
    }
}



