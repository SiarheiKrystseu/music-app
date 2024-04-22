package com.krystseu.microservices.resourceservice.service.impl;

import com.krystseu.microservices.resourceservice.dto.ResourceResponse;
import com.krystseu.microservices.resourceservice.exception.FileParsingException;
import com.krystseu.microservices.resourceservice.exception.FileUploadingException;
import com.krystseu.microservices.resourceservice.exception.InvalidFileException;
import com.krystseu.microservices.resourceservice.exception.SongServiceException;
import com.krystseu.microservices.resourceservice.model.Resource;
import com.krystseu.microservices.resourceservice.repository.ResourceRepository;
import com.krystseu.microservices.resourceservice.service.ResourceService;
import com.krystseu.microservices.songservice.dto.SongRequest;
import com.krystseu.microservices.songservice.dto.SongResponse;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.mp3.Mp3Parser;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Blob;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
@Slf4j
public class ResourceServiceImpl implements ResourceService {

    private final ResourceRepository resourceRepository;
    private final WebClient webClient;
    //todo use Service Discovery instead of hardcoded url
    private static final String SONG_SERVICE_ENDPOINT = "http://localhost:8081/api/songs";

    @Autowired
    public ResourceServiceImpl(ResourceRepository resourceRepository, WebClient webClient) {
        this.resourceRepository = resourceRepository;
        this.webClient = webClient;
    }

    @Override
    public Optional<ResourceResponse> getResourceById(Integer id) {
        Optional<Resource> resourceOptional = resourceRepository.findById(Long.valueOf(id));
        return resourceOptional.map(this::convertToResourceResponse);
    }

    @Override
    public List<Integer> deleteResources(List<Integer> ids) {
        checkSongServiceAvailability();
        List<Integer> deletedIds = new ArrayList<>();
        for (Integer id : ids) {
            log.info("Attempting to delete resource with id {}", id);
            if (resourceRepository.existsById(Long.valueOf(id)) && getSongByResourceId(id).isPresent()) {
                resourceRepository.deleteById(Long.valueOf(id));
                deletedIds.add(id);
                log.info("Successfully deleted resource with id {}", id);
            } else {
                log.warn("Resource with id {} does not exist or no song metadata found", id);
            }
        }
        return deletedIds;
    }

    @Override
    public Optional<SongResponse> getSongByResourceId(Integer id) {
        try {
            return Optional.ofNullable(
                    webClient.get()
                            .uri(SONG_SERVICE_ENDPOINT + "/" + id)
                            .retrieve()
                            .bodyToMono(SongResponse.class)
                            .block()
            );
        } catch (WebClientResponseException.NotFound e) {
            return Optional.empty();
        } catch (WebClientRequestException e) {
            throw new SongServiceException("Error while calling the song service", e);
        }
    }
    @Override
    public Optional<ResourceResponse> uploadFile(MultipartFile file) {
        try {
            Metadata metadata = extractMetadata(file);
            checkSongServiceAvailability();
            Resource savedResource = saveResource(file);
            SongRequest songRequest = createSongRequest(metadata, savedResource);

            log.info("Retrieved file metadata {}", songRequest);
            SongResponse createdSong = createSong(songRequest);
            log.info("Created song with Id {}", createdSong.getId());

            return Optional.ofNullable(convertToResourceResponse(savedResource));
        } catch (Exception e) {
            log.error("Error while processing the file or creating the song", e);
            return Optional.empty();
        }
    }

    private Metadata extractMetadata(MultipartFile file) {
        try {
            InputStream input = file.getInputStream();
            ContentHandler handler = new BodyContentHandler();
            Metadata metadata = new Metadata();
            new Mp3Parser().parse(input, handler, metadata, new ParseContext());
            return metadata;
        } catch (IOException | TikaException | SAXException e) {
            throw new FileParsingException("Error while parsing the file", e);
        }
    }

    private Resource saveResource(MultipartFile file) {
        try {
            byte[] byteData = file.getBytes();
            Blob data = new javax.sql.rowset.serial.SerialBlob(byteData);
            Resource resource = new Resource();
            resource.setData(data);
            return resourceRepository.save(resource);
        } catch (IOException | SQLException e) {
            throw new FileUploadingException("Error while uploading the file", (IOException) e);
        }
    }

    private SongRequest createSongRequest(Metadata metadata, Resource savedResource) {
        String durationStr = metadata.get("xmpDM:duration");
        double duration = 0.0;
        if (durationStr != null) {
            duration = Double.parseDouble(durationStr);
        }
        return SongRequest.builder()
                .name(metadata.get("xmpDM:name"))
                .artist(metadata.get("xmpDM:artist"))
                .album(metadata.get("xmpDM:album"))
                .length(formatDuration(duration))
                .resourceId(savedResource.getId())
                .year(metadata.get("xmpDM:releaseDate"))
                .build();
    }

    private SongResponse createSong(SongRequest songRequest) {
        try {
            return webClient.post()
                    .uri(SONG_SERVICE_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Mono.just(songRequest), SongRequest.class)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, clientResponse ->
                            Mono.error(new SongServiceException("Client Error while calling song service: " + clientResponse.statusCode(), null)))
                    .onStatus(HttpStatusCode::is5xxServerError, clientResponse ->
                            Mono.error(new SongServiceException("Server Error while calling song service: " + clientResponse.statusCode(), null)))
                    .bodyToMono(SongResponse.class)
                    .block();
        } catch (WebClientRequestException e) {
            throw new SongServiceException("Error while calling the song service", e);
        }
    }

    private ResourceResponse convertToResourceResponse(Resource resource) {
        byte[] data = null;
        if (resource.getData() != null) {
            try {
                data = resource.getData().getBytes(1, (int) resource.getData().length());
            } catch (SQLException e) {
                log.error("Error while converting Blob to byte[]", e);
                throw new InvalidFileException("Error while processing the file data", e);
            }
        }
        return ResourceResponse.builder()
                .id(resource.getId())
                .data(data)
                .build();
    }

    public static String formatDuration(double durationInSeconds) {
        int hours = (int) (durationInSeconds / 3600);
        int minutes = (int) ((durationInSeconds % 3600) / 60);

        return String.format("%02d:%02d", hours, minutes);
    }

    private void checkSongServiceAvailability() {
        try {
            HttpStatusCode status = webClient.get()
                    .uri(SONG_SERVICE_ENDPOINT)
                    .exchangeToMono(response -> Mono.just(response.statusCode()))
                    .block();

            if (status != HttpStatus.OK) {
                log.error("Song service is not available. Status code: {}", status);
                throw new SongServiceException("Song service is not available");
            }
        } catch (WebClientRequestException e) {
            log.error("Error while checking song service availability", e);
            throw new SongServiceException("Error while checking song service availability", e);
        }
    }
}