package com.krystseu.microservices.resourceprocessor.service;

import com.krystseu.microservices.songservice.dto.SongRequest;
import org.apache.tika.metadata.Metadata;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashMap;
import java.util.Map;

@Service
public class SongServiceClient {

    private final WebClient.Builder webClientBuilder;
    private final String songServiceEndpoint;

    @Autowired
    public SongServiceClient(WebClient.Builder webClientBuilder,
                             @Value("${song-service.endpoint}") String songServiceEndpoint) {
        this.webClientBuilder = webClientBuilder;
        this.songServiceEndpoint = songServiceEndpoint;
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
            duration = Double.parseDouble(durationStr);
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
                .block();
    }

    private String formatDuration(double duration) {
        // Assuming duration is in seconds
        int minutes = (int) (duration / 60);
        int seconds = (int) (duration % 60);
        return String.format("%d:%02d", minutes, seconds);
    }
}
