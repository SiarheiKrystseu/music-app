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

        log.info("Received message with resource ID: {} and correlation ID: {}", resourceId, correlationId);

        try {
            // Get resource data from Resource Service
            log.info("Fetching resource data for ID: {}", resourceId);
            byte[] resourceData = resourceServiceClient.getResourceData(resourceId);
            if (resourceData == null) {
                log.error("Resource data for ID {} is null", resourceId);
                throw new FileParsingException("Resource data cannot be null");
            }

            // Extract metadata from resource data
            log.info("Extracting metadata from resource data");
            Metadata metadata = extractMetadata(resourceData);

            // Save metadata in Song Service
            log.info("Saving metadata to Song Service");
            songServiceClient.saveMetadata(metadata, Long.valueOf(resourceId));
            log.info("Successfully processed resource with ID: {}", resourceId);

            sendAcknowledgment(resourceId, correlationId);
            log.info("Sent acknowledgment for resource ID: {} with correlation ID: {}", resourceId, correlationId);
        } catch (FileParsingException e) {
            log.error("Failed to process resource with ID: {}. Reason: {}", resourceId, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error occurred while processing resource with ID: {}", resourceId, e);
            throw new RuntimeException(e);
        }
    }

    private void sendAcknowledgment(String resourceId, String correlationId) {
        log.info("Sending acknowledgment for resource ID: {} with correlation ID: {}", resourceId, correlationId);

        MessageProperties messageProperties = new MessageProperties();
        messageProperties.setCorrelationId(correlationId);
        Message message = new Message(resourceId.getBytes(StandardCharsets.UTF_8), messageProperties);

        rabbitTemplate.send(resourceAckQueue, message);
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



