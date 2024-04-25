package com.krystseu.microservices.resourceservice.controller;

import com.krystseu.microservices.resourceservice.dto.ResourceResponse;
import com.krystseu.microservices.resourceservice.exception.AudioUploadingException;
import com.krystseu.microservices.resourceservice.exception.InvalidFileException;
import com.krystseu.microservices.resourceservice.exception.ResourceNotFoundException;
import com.krystseu.microservices.resourceservice.service.ResourceService;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.security.InvalidParameterException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/resources")
public class ResourceController {

    private final ResourceService resourceService;

    @Autowired
    public ResourceController(ResourceService resourceService) {
        this.resourceService = resourceService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getResource(@PathVariable Integer id) {
        Optional<ResourceResponse> resourceOptional = resourceService.getResourceById(id);
        return resourceOptional
                .map(resource -> ResponseEntity.ok().contentType(MediaType.parseMediaType("audio/mpeg")).body(resource.getData()))
                .orElseThrow(() -> new ResourceNotFoundException("The resource with the specified id does not exist"));
    }

    @DeleteMapping
    public ResponseEntity<?> deleteResources(@RequestParam("ids") String idsCSV) {
        if (idsCSV.length() >= 200) {
            throw new InvalidParameterException("CSV length must be less than 200 characters");
        }
        try {
            List<Integer> deletedIds = resourceService.deleteResources(idsCSV);
            return ResponseEntity.ok().body(Collections.singletonMap("ids", deletedIds));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping(path = "/upload", consumes = "audio/mpeg")
    public ResponseEntity<?> uploadAudioFile(@RequestBody byte[] audioData) throws TikaException, IOException, SAXException {
        validateAudioData(audioData);
        Optional<ResourceResponse> resourceResponseOptional = resourceService.uploadAudio(audioData);
        return resourceResponseOptional
                .map(response -> ResponseEntity.status(HttpStatus.CREATED).body(Collections.singletonMap("id", response.getId())))
                .orElseThrow(() -> new AudioUploadingException("Failed to upload audio file"));
    }

    private void validateAudioData(byte[] audioData) {
        if (audioData == null || audioData.length == 0) {
            throw new InvalidFileException("Invalid audio: Uploaded audio data cannot be empty");
        }
        Tika tika = new Tika();

        // Detect the content type of the audio data
        String contentType = tika.detect(audioData);

        // Check if the detected content type indicates an MP3 audio file
        if (!contentType.equals("audio/mpeg")) {
            throw new InvalidFileException("Invalid MP3: The audio data does not appear to be in MP3 format");
        }
    }
}

