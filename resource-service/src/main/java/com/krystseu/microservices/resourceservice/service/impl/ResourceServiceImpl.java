package com.krystseu.microservices.resourceservice.service.impl;

import com.amazonaws.services.s3.model.*;
import com.krystseu.microservices.resourceservice.dto.ResourceResponse;
import com.krystseu.microservices.resourceservice.exception.AudioUploadingException;
import com.krystseu.microservices.resourceservice.exception.InvalidFileException;
import com.krystseu.microservices.resourceservice.exception.ResourceNotFoundException;
import com.krystseu.microservices.resourceservice.model.Resource;
import com.krystseu.microservices.resourceservice.repository.ResourceRepository;
import com.krystseu.microservices.resourceservice.service.*;
import com.krystseu.microservices.storageservice.model.Storage;
import com.krystseu.microservices.storageservice.model.StorageType;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.annotation.Recover;
import org.springframework.stereotype.Service;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;
import com.amazonaws.services.s3.AmazonS3;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.web.client.HttpClientErrorException;

@Service
@Slf4j
public class ResourceServiceImpl implements ResourceService {


    private final ResourceRepository resourceRepository;
    private final AmazonS3 amazonS3;
    private final RabbitTemplate rabbitTemplate;
    private final StorageServiceClient storageServiceClient;
    private final String processQueue;
    private final String ackQueue;

    @Autowired
    public ResourceServiceImpl(ResourceRepository resourceRepository,
                               AmazonS3 amazonS3,
                               RabbitTemplate rabbitTemplate,
                               StorageServiceClient storageServiceClient,
                               @Qualifier("resourceQueueName") String processQueue,
                               @Qualifier("ackQueueName") String ackQueue) {
        this.resourceRepository = resourceRepository;
        this.amazonS3 = amazonS3;
        this.rabbitTemplate = rabbitTemplate;
        this.storageServiceClient = storageServiceClient;
        this.processQueue = processQueue;
        this.ackQueue = ackQueue;
    }

    @Override
    public Optional<ResourceResponse> getResourceById(Integer id) {
        return resourceRepository.findById(Long.valueOf(id))
                .map(this::convertToResourceResponse);
    }

    @Override
    @Transactional
    public List<Integer> deleteResources(String idsCSV) {
        List<Integer> ids = parseIds(idsCSV);
        List<Integer> deletedIds = new ArrayList<>();
        for (Integer id : ids) {
            log.info("Attempting to delete resource with id {}", id);
            resourceRepository.findById(Long.valueOf(id)).ifPresent(resource -> {
                Storage storage = storageServiceClient.getStorageByType(resource.getStorageType());
                String bucketName = storage.getBucket();
                String key = extractKeyFromLocation(resource.getLocation(), storage.getPath());
                amazonS3.deleteObject(bucketName, key);
                resourceRepository.deleteById(Long.valueOf(id));
                deletedIds.add(id);
                log.info("Successfully deleted resource with id {}", id);
            });
        }
        return deletedIds;
    }

    private List<Integer> parseIds(String idsCSV) {
        try {
            return Arrays.stream(idsCSV.split(","))
                    .map(Integer::parseInt)
                    .collect(Collectors.toList());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid CSV format: all values must be integers", e);
        }
    }

    @Override
    public Optional<ResourceResponse> uploadAudio(byte[] audioData) {
        try {
            String fileName = uploadToCloudStorage(audioData, StorageType.STAGING);
            Resource savedResource = saveResourceAndCommit(fileName, StorageType.STAGING);

            log.info("Uploaded audio to cloud storage at {}", savedResource.getLocation());
            log.info("Saved resource with ID {}", savedResource.getId());

            // Process the resource asynchronously
            processResourceAsync(savedResource);

            return Optional.ofNullable(convertToResourceResponse(savedResource));
        } catch (Exception e) {
            log.error("Error while uploading the audio", e);
            return Optional.empty();
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, isolation = Isolation.READ_COMMITTED)
    public Resource saveResourceAndCommit(String fileName, StorageType storageType) {
        log.info("Starting transaction for saving resource.");
        Storage storage = storageServiceClient.getStorageByType(storageType);
        String locationUrl = amazonS3.getUrl(storage.getBucket(), formatS3Key(storage.getPath(), fileName)).toString();

        Resource resource = new Resource();
        resource.setLocation(locationUrl);
        resource.setStorageType(storageType);

        log.info("Saving resource with filename {} at {}", fileName, locationUrl);
        Resource savedResource = resourceRepository.save(resource);
        resourceRepository.flush();

        // Log transaction commit and check visibility
        log.info("Resource saved and transaction flushed. Resource ID: {}", savedResource.getId());

        return savedResource;
    }

    @Async
    public void processResourceAsync(Resource savedResource) {
        String correlationId = UUID.randomUUID().toString();
        sendMessageToProcessQueue(savedResource.getId().toString(), correlationId);

        try {
            // Wait for acknowledgment
            boolean processed = waitForResourceProcessing(savedResource.getId(), correlationId);
            if (processed) {
                log.info("Resource with ID {} processed successfully.", savedResource.getId());
            } else {
                log.warn("Resource with ID {} processing failed after retries.", savedResource.getId());
            }
        } catch (InterruptedException e) {
            log.error("Processing interrupted for resource ID: {}", savedResource.getId(), e);
            Thread.currentThread().interrupt();
        }
    }

    private void sendMessageToProcessQueue(String resourceId, String correlationId) {
        MessageProperties messageProperties = new MessageProperties();
        messageProperties.setCorrelationId(correlationId);
        messageProperties.setHeader("replyTo", ackQueue);

        Message message = new Message(resourceId.getBytes(StandardCharsets.UTF_8), messageProperties);

        rabbitTemplate.convertAndSend("resource.exchange", "process.routing.key", message);
        log.info("Sent message to process queue with resource ID: {} and correlation ID: {}", resourceId, correlationId);
    }

    private boolean waitForResourceProcessing(Long resourceId, String correlationId) throws InterruptedException {
        final int maxAttempts = 10;
        final long initialWaitTime = 1000;

        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            boolean success = attemptToProcessResource(correlationId);
            if (success) {
                Resource resource = getResource(resourceId);
                if (resource != null && resource.getStorageType() == StorageType.PERMANENT) {
                    return true;
                }
            }
            long waitTime = initialWaitTime * (long) Math.pow(2, attempt);
            Thread.sleep(waitTime);
        }

        return false; // Processing failed after retries
    }

    private boolean attemptToProcessResource(String correlationId) {
        Message response = rabbitTemplate.receive(ackQueue, 1000);
        if (response != null && correlationId.equals(response.getMessageProperties().getCorrelationId())) {
            return handleAcknowledgment(response);
        }
        return false;
    }

    private boolean handleAcknowledgment(Message response) {
        String responseResourceId = new String(response.getBody(), StandardCharsets.UTF_8);
        log.info("Received response for resource with id: {}", responseResourceId);
        Resource resource = getResource(Long.valueOf(responseResourceId));
        if (resource != null) {
            moveResourceToPermanent(resource.getId());
            return true;
        } else {
            log.warn("Resource with ID {} not found.", responseResourceId);
            return false;
        }
    }

    private String uploadToCloudStorage(byte[] audioData, StorageType storageType) {
        String fileName = UUID.randomUUID().toString() + ".mp3";
        Storage storage = storageServiceClient.getStorageByType(storageType);
        String bucket = storage.getBucket();
        String path = storage.getPath();

        // Generate the full S3 key
        String s3Key = formatS3Key(path, fileName);

        try (InputStream input = new ByteArrayInputStream(audioData)) {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(audioData.length);
            amazonS3.putObject(new PutObjectRequest(bucket, s3Key, input, metadata)
                    .withCannedAcl(CannedAccessControlList.Private));
            return fileName;
        } catch (IOException e) {
            log.error("Error while uploading the file to cloud storage", e);
            throw new AudioUploadingException("Error while uploading the file to cloud storage", e);
        }
    }

    public void moveResourceToPermanent(Long resourceId) {
        log.info("Starting to move resource with ID: {}", resourceId);

        try {
            Resource resource = getResource(resourceId);
            Storage stagingStorage = storageServiceClient.getStorageByType(StorageType.STAGING);
            Storage permanentStorage = storageServiceClient.getStorageByType(StorageType.PERMANENT);

            String originalKey = extractKeyFromLocation(resource.getLocation(), stagingStorage.getPath());
            String newKey = formatS3Key(permanentStorage.getPath(), originalKey);

            copyResourceToNewLocation(originalKey, newKey);
            updateResourceLocationAndSave(resource, newKey, permanentStorage);
        } catch (IllegalArgumentException e) {
            log.error("Input validation failed for resource ID: {}", resourceId, e);
            throw new AudioUploadingException("Input validation failed", e);
        } catch (RuntimeException e) {
            log.error("Failed to move resource with ID: {}", resourceId, e);
            throw new AudioUploadingException("Failed to move resource to permanent storage", e);
        }
    }

    Resource getResource(Long resourceId) {
        return resourceRepository.findById(resourceId)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found with ID: " + resourceId));
    }

    private void copyResourceToNewLocation(String originalKey, String newKey) {
        String stagingBucketName = getBucketNameByStorageType(StorageType.STAGING);
        String permanentBucketName = getBucketNameByStorageType(StorageType.PERMANENT);

        log.info("Initiating object copy from {} to {}/{}", stagingBucketName, permanentBucketName, newKey);
        amazonS3.copyObject(stagingBucketName, originalKey, permanentBucketName, newKey);
        log.info("Object copied successfully. Deleting original object at {}", originalKey);

        amazonS3.deleteObject(stagingBucketName, originalKey);
        log.info("Original object deleted successfully.");
    }

    private void updateResourceLocationAndSave(Resource resource, String newKey, Storage permanentStorage) {
        String newLocationUrl = amazonS3.getUrl(permanentStorage.getBucket(), newKey).toString();
        resource.setLocation(newLocationUrl);
        resource.setStorageType(StorageType.PERMANENT);
        resourceRepository.save(resource);
        resourceRepository.flush();

        log.info("Resource with ID {} moved to PERMANENT storage at {}", resource.getId(), newLocationUrl);
    }

    public String formatS3Key(String basePath, String originalKey) {
        // Ensure basePath does not start or end with a slash
        basePath = basePath.replaceAll("^/+", "").replaceAll("/+$", "");
        // Ensure originalKey does not start with a slash
        originalKey = originalKey.replaceAll("^/+", "");

        // Split the originalKey to remove any existing bucket name or unwanted paths
        String[] keyParts = originalKey.split("/");
        String actualKey = keyParts[keyParts.length - 1]; // Get the actual key (filename)

        // Return the concatenated path with a single slash between basePath and actualKey
        return basePath + "/" + actualKey;
    }


    private ResourceResponse convertToResourceResponse(Resource resource) {
        try {
            log.info("Converting resource ID {} to ResourceResponse", resource.getId());
            Storage storage = storageServiceClient.getStorageByType(resource.getStorageType());
            String key = extractKeyFromLocation(resource.getLocation(), storage.getPath());
            String bucketName = storage.getBucket();
            log.info("Retrieving object with key {} from bucket {}", key, bucketName);
            S3Object s3Object = amazonS3.getObject(bucketName, key);

            byte[] data = IOUtils.toByteArray(s3Object.getObjectContent());
            log.info("Successfully retrieved and processed resource ID {}", resource.getId());
            return ResourceResponse.builder()
                    .id(resource.getId())
                    .data(data)
                    .build();
        } catch (AmazonS3Exception e) {
            log.error("S3 Error while retrieving file for resource ID {}: Error Code: {}, Error Message: {}",
                    resource.getId(), e.getErrorCode(), e.getErrorMessage(), e);
            throw new InvalidFileException("Error while processing the file data", e);
        } catch (IOException e) {
            log.error("I/O Error while retrieving file from cloud storage for resource ID {}", resource.getId(), e);
            throw new InvalidFileException("Error while processing the file data", e);
        }
    }

    public String extractKeyFromLocation(String location, String basePath) {
        try {
            // Create a URL object from the location string
            URL url = new URL(location);

            // Get the path part of the URL
            String path = url.getPath();

            // Ensure basePath starts with '/'
            String basePathWithSlash = basePath.startsWith("/") ? basePath : "/" + basePath;

            // Remove any initial slashes from basePath for comparison
            String basePathTrimmed = basePathWithSlash.replaceFirst("^/+", "");

            // Remove any segments preceding the basePath
            int index = path.indexOf(basePathTrimmed);
            if (index != -1) {
                // Return the key which is the part of the path starting from basePath
                return path.substring(index);
            } else {
                throw new IllegalArgumentException("Location path does not contain the base path.");
            }
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Invalid URL provided.", e);
        }
    }

    private String getBucketNameByStorageType(StorageType storageType) {
        Storage storage = storageServiceClient.getStorageByType(storageType);
        return storage.getBucket();
    }


    @Recover
    public Resource recover(HttpClientErrorException e, Integer id) {
        log.error("Failed to retrieve resource with ID: {}", id, e);
        throw new ResourceNotFoundException("Failed to retrieve resource with ID: " + id);
    }
}
