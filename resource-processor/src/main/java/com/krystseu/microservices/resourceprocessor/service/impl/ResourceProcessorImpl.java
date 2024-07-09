package com.krystseu.microservices.resourceprocessor.service.impl;

import com.krystseu.microservices.resourceprocessor.exception.FileParsingException;
import com.krystseu.microservices.resourceprocessor.service.ResourceProcessor;
import com.krystseu.microservices.resourceprocessor.service.ResourceServiceClient;
import com.krystseu.microservices.resourceprocessor.service.SongServiceClient;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.mp3.Mp3Parser;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

@Service
@Slf4j
public class ResourceProcessorImpl implements ResourceProcessor {

    private final ResourceServiceClient resourceServiceClient;
    private final SongServiceClient songServiceClient;
    private final Tika tika;

    @Autowired
    public ResourceProcessorImpl(ResourceServiceClient resourceServiceClient, SongServiceClient songServiceClient) {
        this.resourceServiceClient = resourceServiceClient;
        this.songServiceClient = songServiceClient;
        this.tika = new Tika();
    }
    @RabbitListener(queues = "${resource.rabbitmq.queue}")
    @Retryable(value = {WebClientResponseException.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000))
    public void processResourceMessage(String resourceId) {
        log.info("Received message with resource ID: {}", resourceId);

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
        } catch (FileParsingException e) {
            log.error("Failed to process resource with ID: {}. Reason: {}", resourceId, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error occurred while processing resource with ID: {}", resourceId, e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public Metadata extractMetadata(byte[] audioData) {
        try (InputStream input = new ByteArrayInputStream(audioData)) {
            ContentHandler handler = new BodyContentHandler();
            Metadata metadata = new Metadata();
            new Mp3Parser().parse(input, handler, metadata, new ParseContext());
            return metadata;
        } catch (IOException | TikaException | SAXException e) {
            throw new FileParsingException("Error while parsing the audio data", e);
        }
    }

    @Recover
    public void recover(WebClientResponseException.NotFound e, String resourceId) {
        log.error("Failed to retrieve resource with ID: " + resourceId, e);
    }
}
