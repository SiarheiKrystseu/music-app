package com.krystseu.microservices.resourceprocessor.service;

import com.krystseu.microservices.songservice.dto.SongRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.metadata.Metadata;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class SongServiceClient {

    private final WebClient.Builder webClientBuilder;
    private final String songServiceEndpoint;
    private final ApplicationEventPublisher eventPublisher;

    @Autowired
    public SongServiceClient(WebClient.Builder webClientBuilder,
                             @Value("${song-service.endpoint}") String songServiceEndpoint,
                             ApplicationEventPublisher eventPublisher) {
        this.webClientBuilder = webClientBuilder;
        this.songServiceEndpoint = songServiceEndpoint;
        this.eventPublisher = eventPublisher;
    }

    public void saveMetadata(Metadata metadata, Long resourceId) {
        // Convert Metadata to SongRequest
        SongRequest songRequest = new SongRequest();
        songRequest.setName(metadata.get("dc:title"));
        songRequest.setArtist(metadata.get("xmpDM:artist"));
        songRequest.setAlbum(metadata.get("xmpDM:album"));
        // Parse the duration
        String durationStr = metadata.get("xmpDM:duration");
        double duration = 0.0;
        if (durationStr != null) {
            try {
                duration = Double.parseDouble(durationStr);
            } catch (NumberFormatException e) {
                log.error("Invalid duration format: {}", durationStr);
                duration = 0.0; // Default to 0 if parsing fails
            }
        }
        songRequest.setLength(formatDuration(duration));
        songRequest.setResourceId(resourceId);
        songRequest.setYear(metadata.get("xmpDM:releaseDate"));

        webClientBuilder.build().post()
                .uri(songServiceEndpoint)
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(songRequest))
                .retrieve()
                .bodyToMono(Void.class)
                .doOnSuccess(aVoid -> {
                    // Publish an event after metadata is saved
                    eventPublisher.publishEvent(new SongMetadataSavedEvent(resourceId));
                })
                .doOnError(error -> {
                    log.error("Failed to save metadata for resource ID: {}", resourceId, error);
                })
                .block();
    }

    private String formatDuration(double duration) {
        // Assuming duration is in seconds
        int minutes = (int) (duration / 60);
        int seconds = (int) (duration % 60);
        return String.format("%d:%02d", minutes, seconds);
    }
}


