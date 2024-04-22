package com.krystseu.microservices.resourceservice.controller;

import com.krystseu.microservices.resourceservice.dto.ResourceResponse;
import com.krystseu.microservices.resourceservice.exception.FileUploadingException;
import com.krystseu.microservices.resourceservice.exception.InvalidFileException;
import com.krystseu.microservices.resourceservice.exception.ResourceNotFoundException;
import com.krystseu.microservices.resourceservice.service.ResourceService;
import org.apache.tika.exception.TikaException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
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
    public ResponseEntity<?> deleteResources(@RequestParam("id") List<Integer> ids) {
        if (ids.toString().length() >= 200) {
            throw new InvalidParameterException("CSV length must be less than 200 characters");
        }
        try {
            List<Integer> deletedIds = resourceService.deleteResources(ids);
            return ResponseEntity.ok().body(Collections.singletonMap("ids", deletedIds));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/upload")
    public ResponseEntity<?> uploadFile(@RequestParam("file") MultipartFile file) throws TikaException, IOException, SAXException {
        validateFile(file);
        Optional<ResourceResponse> resourceResponseOptional = resourceService.uploadFile(file);
        return resourceResponseOptional
                .map(response -> ResponseEntity.status(HttpStatus.CREATED).body(Collections.singletonMap("id", response.getId())))
                .orElseThrow(() -> new FileUploadingException("Failed to upload file"));
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidFileException("Invalid MP3: Uploaded file cannot be empty");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.endsWith(".mp3")) {
            throw new InvalidFileException("Invalid MP3: The file type must be MP3");
        }

        if (!file.getContentType().equals("audio/mpeg")) {
            throw new InvalidFileException("Invalid MP3: The file content must be in MP3 format");
        }
    }
}

