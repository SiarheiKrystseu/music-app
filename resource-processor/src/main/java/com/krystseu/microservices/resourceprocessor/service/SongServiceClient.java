package com.krystseu.microservices.resourceprocessor.service;

import com.krystseu.microservices.resourceprocessor.firebase.FirebaseAuthUtils;
import com.krystseu.microservices.songservice.dto.SongRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.metadata.Metadata;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;

@Service
@Slf4j
public class SongServiceClient {

    private final WebClient.Builder webClientBuilder;
    private final String songServiceEndpoint;
    private final ApplicationEventPublisher eventPublisher;
    private final RabbitTemplate rabbitTemplate;
    private final FirebaseAuthUtils firebaseAuthUtils;

    @Autowired
    public SongServiceClient(WebClient.Builder webClientBuilder,
                             @Value("${song-service.endpoint}") String songServiceEndpoint,
                             ApplicationEventPublisher eventPublisher,
                             RabbitTemplate rabbitTemplate,
                             FirebaseAuthUtils firebaseAuthUtils) {
        this.webClientBuilder = webClientBuilder;
        this.songServiceEndpoint = songServiceEndpoint;
        this.eventPublisher = eventPublisher;
        this.rabbitTemplate = rabbitTemplate;
        this.firebaseAuthUtils = firebaseAuthUtils;
    }

    public void saveMetadata(Metadata metadata, Long resourceId) {
        SongRequest songRequest = createSongRequestFromMetadata(metadata, resourceId);
        String traceId = MDC.get("traceId");
        String authToken = firebaseAuthUtils.getAuthTokenFromContext();

        log.debug("Sending metadata to song service for resource ID: {}, Trace ID: {}", resourceId, traceId);

        webClientBuilder.build()
                .post()
                .uri(songServiceEndpoint)
                .header("Authorization", "Bearer " + authToken)
                .header("X-Trace-ID", traceId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(songRequest))
                .retrieve()
                .bodyToMono(Void.class)
                .doOnSuccess(aVoid -> {
                    log.info("Successfully saved metadata for resource ID: {}", resourceId);
                    eventPublisher.publishEvent(new SongMetadataSavedEvent(resourceId));
                    sendResponseBackToResourceService("Response Object", "queueName");
                })
                .doOnError(error -> {
                    log.error("Failed to save metadata for resource ID: {}", resourceId, error);
                })
                .block();
    }

    private void sendResponseBackToResourceService(Object response, String queueName) {
        String traceId = MDC.get("traceId");
        MessageProperties messageProperties = new MessageProperties();
        messageProperties.setHeader("X-Trace-ID", traceId);
        Message message = new Message(convertObjectToJsonBytes(response), messageProperties);
        rabbitTemplate.convertAndSend(queueName, message);
    }

    private byte[] convertObjectToJsonBytes(Object object) {
        try {
            return new ObjectMapper().writeValueAsBytes(object);
        } catch (JsonProcessingException e) {
            log.error("Failed to convert object to JSON bytes", e);
            return new byte[0];
        }
    }

    private SongRequest createSongRequestFromMetadata(Metadata metadata, Long resourceId) {
        SongRequest songRequest = new SongRequest();
        songRequest.setName(metadata.get("dc:title"));
        songRequest.setArtist(metadata.get("xmpDM:artist"));
        songRequest.setAlbum(metadata.get("xmpDM:album"));
        songRequest.setYear(metadata.get("xmpDM:releaseDate"));
        songRequest.setLength(formatDuration(parseDouble(metadata.get("xmpDM:duration"))));
        songRequest.setResourceId(resourceId);
        return songRequest;
    }

    private double parseDouble(String value) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            log.error("Invalid number format: {}", value);
            return 0.0;
        }
    }

    private String formatDuration(double duration) {
        int minutes = (int) (duration / 60);
        int seconds = (int) (duration % 60);
        return String.format("%d:%02d", minutes, seconds);
    }
}
