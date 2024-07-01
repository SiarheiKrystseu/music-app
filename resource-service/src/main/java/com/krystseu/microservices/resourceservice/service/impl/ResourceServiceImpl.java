package com.krystseu.microservices.resourceservice.service.impl;

import com.amazonaws.services.s3.model.ObjectMetadata;
import com.krystseu.microservices.resourceservice.dto.ResourceResponse;
import com.krystseu.microservices.resourceservice.exception.AudioUploadingException;
import com.krystseu.microservices.resourceservice.exception.InvalidFileException;
import com.krystseu.microservices.resourceservice.model.Resource;
import com.krystseu.microservices.resourceservice.repository.ResourceRepository;
import com.krystseu.microservices.resourceservice.service.ResourceService;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import org.springframework.beans.factory.annotation.Value;

@Service
@Transactional
@Slf4j
public class ResourceServiceImpl implements ResourceService {

    private final ResourceRepository resourceRepository;
    private final AmazonS3 amazonS3;

    @Value("${cloud.aws.s3.bucket-name}")
    String bucketName;

    @Autowired
    public ResourceServiceImpl(ResourceRepository resourceRepository, AmazonS3 amazonS3) {
        this.resourceRepository = resourceRepository;
        this.amazonS3 = amazonS3;
    }

    @Override
    public Optional<ResourceResponse> getResourceById(Integer id) {
        Optional<Resource> resourceOptional = resourceRepository.findById(Long.valueOf(id));
        return resourceOptional.map(this::convertToResourceResponse);
    }

    @Override
    public List<Integer> deleteResources(String idsCSV) {
        List<Integer> ids;
        try {
            ids = Arrays.stream(idsCSV.split(","))
                    .map(Integer::parseInt)
                    .toList();
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid CSV format: all values must be integers");
        }

        List<Integer> deletedIds = new ArrayList<>();
        for (Integer id : ids) {
            log.info("Attempting to delete resource with id {}", id);
            if (resourceRepository.existsById(Long.valueOf(id))) {
                Resource resource = resourceRepository.findById(Long.valueOf(id)).get();
                amazonS3.deleteObject(bucketName, resource.getLocation());
                resourceRepository.deleteById(Long.valueOf(id));
                deletedIds.add(id);
                log.info("Successfully deleted resource with id {}", id);
            } else {
                log.warn("Resource with id {} does not exist", id);
            }
        }
        return deletedIds;
    }

    @Override
    public Optional<ResourceResponse> uploadAudio(byte[] audioData) {
        try {
            String fileName = uploadToCloudStorage(audioData);
            Resource savedResource = saveResource(fileName);

            log.info("Uploaded audio to cloud storage at {}", savedResource.getLocation());
            log.info("Saved resource with ID {}", savedResource.getId());

            return Optional.ofNullable(convertToResourceResponse(savedResource));
        } catch (Exception e) {
            log.error("Error while uploading the audio", e);
            return Optional.empty();
        }
    }

    private String uploadToCloudStorage(byte[] audioData) {
        String fileName = UUID.randomUUID().toString() + ".mp3";
        try (InputStream input = new ByteArrayInputStream(audioData)) {
            amazonS3.putObject(new PutObjectRequest(bucketName, fileName, input, new ObjectMetadata())
                    .withCannedAcl(CannedAccessControlList.Private));
            return fileName;
        } catch (IOException e) {
            throw new AudioUploadingException("Error while uploading the file to cloud storage", e);
        }
    }

    private Resource saveResource(String fileName) {
        Resource resource = new Resource();
        log.info("Saving resource with filename {}", fileName);
        String locationUrl = amazonS3.getUrl(bucketName, fileName).toString();
        log.info("Saving resource with locationUrl {}", locationUrl);
        resource.setLocation(locationUrl);
        return resourceRepository.save(resource);
    }

    private ResourceResponse convertToResourceResponse(Resource resource) {
        byte[] data = null;
        try {
            String key = extractKeyFromLocation(resource.getLocation());
            log.info("Retrieving object with key {}", key);
            S3Object s3Object = amazonS3.getObject(bucketName, key);

            data = IOUtils.toByteArray(s3Object.getObjectContent());
        } catch (IOException e) {
            log.error("Error while retrieving file from cloud storage", e);
            throw new InvalidFileException("Error while processing the file data", e);
        }
        return ResourceResponse.builder()
                .id(resource.getId())
                .data(data)
                .build();
    }

    public String extractKeyFromLocation(String location) {
        return location.substring(location.lastIndexOf("/") + 1);
    }

    public static String formatDuration(double durationInSeconds) {
        int hours = (int) (durationInSeconds / 3600);
        int minutes = (int) ((durationInSeconds % 3600) / 60);
        return String.format("%02d:%02d", hours, minutes);
    }
}
