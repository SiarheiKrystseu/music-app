package com.krystseu.microservices.resourceservice.service.impl;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.*;
import com.krystseu.microservices.resourceservice.dto.ResourceResponse;
import com.krystseu.microservices.resourceservice.exception.AudioUploadingException;
import com.krystseu.microservices.resourceservice.exception.ResourceNotFoundException;
import com.krystseu.microservices.resourceservice.model.Resource;
import com.krystseu.microservices.resourceservice.repository.ResourceRepository;
import com.krystseu.microservices.resourceservice.service.StorageServiceClient;
import com.krystseu.microservices.storageservice.model.Storage;
import com.krystseu.microservices.storageservice.model.StorageType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResourceServiceImplTest {

    @Mock
    private ResourceRepository resourceRepository;

    @Mock
    private AmazonS3 amazonS3;

    @Mock
    private StorageServiceClient storageServiceClient;

    @InjectMocks
    private ResourceServiceImpl resourceService;

    private final String stagingBucketName = "staging-bucket";
    private final String permanentBucketName = "permanent-bucket";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(resourceService, "stagingBucketName", stagingBucketName);
        ReflectionTestUtils.setField(resourceService, "permanentBucketName", permanentBucketName);
    }

    @Test
    void testGetResourceById() throws IOException {
        // Mock data
        Long id = 1L;
        String location = "http://localhost:4510/" + permanentBucketName + "/files/" + UUID.randomUUID().toString() + ".mp3";
        Resource resource = new Resource();
        resource.setId(id);
        resource.setLocation(location);
        resource.setStorageType(StorageType.PERMANENT);

        byte[] audioData = "audio data".getBytes();
        S3Object s3Object = mock(S3Object.class);
        S3ObjectInputStream s3ObjectInputStream = new S3ObjectInputStream(new ByteArrayInputStream(audioData.clone()), null);
        when(s3Object.getObjectContent()).thenReturn(s3ObjectInputStream);
        when(resourceRepository.findById(anyLong())).thenReturn(Optional.of(resource));

        Storage permanentStorage = new Storage();
        permanentStorage.setBucket(permanentBucketName);
        permanentStorage.setPath("files");
        when(storageServiceClient.getStorageByType(StorageType.PERMANENT)).thenReturn(permanentStorage);

        String key = resourceService.extractKeyFromLocation(resource.getLocation(), permanentStorage.getPath());
        when(amazonS3.getObject(permanentBucketName, key)).thenReturn(s3Object);

        // Test
        Optional<ResourceResponse> resourceResponseOptional = resourceService.getResourceById(id.intValue());

        // Assertions
        assertTrue(resourceResponseOptional.isPresent());
        assertEquals(id, resourceResponseOptional.get().getId());
        assertArrayEquals(audioData, resourceResponseOptional.get().getData());
    }

    @Test
    void testDeleteResources() {
        // Mock data
        String idsCSV = "1,2,3";

        String location1 = "http://localhost:4510/" + permanentBucketName + "/files/" + UUID.randomUUID().toString() + ".mp3";
        String location2 = "http://localhost:4510/" + permanentBucketName + "/files/" + UUID.randomUUID().toString() + ".mp3";
        String location3 = "http://localhost:4510/" + permanentBucketName + "/files/" + UUID.randomUUID().toString() + ".mp3";

        Resource resource1 = new Resource();
        resource1.setId(1L);
        resource1.setLocation(location1);
        resource1.setStorageType(StorageType.PERMANENT);

        Resource resource2 = new Resource();
        resource2.setId(2L);
        resource2.setLocation(location2);
        resource2.setStorageType(StorageType.PERMANENT);

        Resource resource3 = new Resource();
        resource3.setId(3L);
        resource3.setLocation(location3);
        resource3.setStorageType(StorageType.PERMANENT);

        when(resourceRepository.findById(1L)).thenReturn(Optional.of(resource1));
        when(resourceRepository.findById(2L)).thenReturn(Optional.of(resource2));
        when(resourceRepository.findById(3L)).thenReturn(Optional.of(resource3));

        Storage permanentStorage = new Storage();
        permanentStorage.setBucket(permanentBucketName);
        permanentStorage.setPath("files");
        when(storageServiceClient.getStorageByType(StorageType.PERMANENT)).thenReturn(permanentStorage);

        String key1 = resourceService.extractKeyFromLocation(location1, permanentStorage.getPath());
        String key2 = resourceService.extractKeyFromLocation(location2, permanentStorage.getPath());
        String key3 = resourceService.extractKeyFromLocation(location3, permanentStorage.getPath());

        doNothing().when(amazonS3).deleteObject(permanentBucketName, key1);
        doNothing().when(amazonS3).deleteObject(permanentBucketName, key2);
        doNothing().when(amazonS3).deleteObject(permanentBucketName, key3);

        doNothing().when(resourceRepository).deleteById(1L);
        doNothing().when(resourceRepository).deleteById(2L);
        doNothing().when(resourceRepository).deleteById(3L);

        // Test
        List<Integer> deletedIds = resourceService.deleteResources(idsCSV);

        // Assertions
        assertNotNull(deletedIds);
        assertEquals(3, deletedIds.size());
        assertTrue(deletedIds.containsAll(List.of(1, 2, 3)));
    }


    @Test
    void testFormatS3Key() {
        // Test data
        String basePath = "/base-path";
        String originalKey = "original-key.mp3";

        // Test case 1: normal case
        String result = resourceService.formatS3Key(basePath, originalKey);
        assertEquals("base-path/original-key.mp3", result);

        // Test case 2: basePath with trailing slash
        result = resourceService.formatS3Key(basePath + "/", originalKey);
        assertEquals("base-path/original-key.mp3", result);

        // Test case 3: originalKey with leading slash
        result = resourceService.formatS3Key(basePath, "/" + originalKey);
        assertEquals("base-path/original-key.mp3", result);

        // Test case 4: both basePath and originalKey with slashes
        result = resourceService.formatS3Key(basePath + "/", "/" + originalKey);
        assertEquals("base-path/original-key.mp3", result);

        // Test case 5: basePath with multiple trailing slashes
        result = resourceService.formatS3Key(basePath + "///", originalKey);
        assertEquals("base-path/original-key.mp3", result);

        // Test case 6: originalKey with multiple leading slashes
        result = resourceService.formatS3Key(basePath, "///" + originalKey);
        assertEquals("base-path/original-key.mp3", result);
    }


    @Test
    void testMoveResourceToPermanent() throws Exception {
        // Setup mock data
        Long resourceId = 1L;
        String originalLocation = "https://bucket.s3.amazonaws.com/staging/files/original-file.mp3";
        String newLocation = "https://bucket.s3.amazonaws.com/permanent/files/original-file.mp3";

        Resource resource = new Resource();
        resource.setId(resourceId);
        resource.setLocation(originalLocation);
        resource.setStorageType(StorageType.STAGING);

        Storage stagingStorage = Storage.builder()
                .bucket("staging-bucket")
                .path("files")
                .storageType(StorageType.STAGING)
                .build();

        Storage permanentStorage = Storage.builder()
                .bucket("permanent-bucket")
                .path("files")
                .storageType(StorageType.PERMANENT)
                .build();

        // Calculate keys from locations
        String originalKey = "files/original-file.mp3";  // Extracted key from originalLocation
        String newKey = "files/original-file.mp3";  // Extracted key for newLocation

        // Set up mocks
        when(resourceRepository.findById(resourceId)).thenReturn(Optional.of(resource));
        when(storageServiceClient.getStorageByType(StorageType.STAGING)).thenReturn(stagingStorage);
        when(storageServiceClient.getStorageByType(StorageType.PERMANENT)).thenReturn(permanentStorage);

        // Stubbing with correct arguments
        when(amazonS3.copyObject(eq("staging-bucket"), eq(originalKey), eq("permanent-bucket"), eq(newKey)))
                .thenReturn(new CopyObjectResult());
        when(amazonS3.getUrl("permanent-bucket", newKey)).thenReturn(new URL(newLocation));

        // Act
        resourceService.moveResourceToPermanent(resourceId);

        // Verify interactions
        verify(amazonS3).copyObject("staging-bucket", originalKey, "permanent-bucket", newKey);
        verify(amazonS3).deleteObject("staging-bucket", originalKey);

        ArgumentCaptor<Resource> resourceCaptor = ArgumentCaptor.forClass(Resource.class);
        verify(resourceRepository).save(resourceCaptor.capture());
        Resource savedResource = resourceCaptor.getValue();

        // Assert
        assertEquals(newLocation, savedResource.getLocation());
        assertEquals(StorageType.PERMANENT, savedResource.getStorageType());

        // Verify no more interactions
        verifyNoMoreInteractions(amazonS3, resourceRepository, storageServiceClient);
    }





    //@Test
    void testMoveResourceToPermanent_ResourceNotFound() {
        // Arrange
        Long resourceId = 100L;

        // Mock the resourceRepository to return an empty Optional
        when(resourceRepository.findById(resourceId)).thenReturn(Optional.empty());

        // Act & Assert: Verify that ResourceNotFoundException is thrown
        ResourceNotFoundException thrownException = assertThrows(ResourceNotFoundException.class, () -> {
            resourceService.moveResourceToPermanent(resourceId);
        });

        // Verify the exception message
        assertEquals("Resource not found with ID: 100", thrownException.getMessage());
    }

    @Test
    void testMoveResourceToPermanent_S3Error() {
        // Setup mock data
        Long resourceId = 1L;
        String originalLocation = "https://bucket-name.s3.amazonaws.com/files/original-file.mp3";
        String originalKey = "files/original-file.mp3";
        String newKey = "permanent/files/original-file.mp3";

        Resource resource = new Resource();
        resource.setId(resourceId);
        resource.setLocation(originalLocation);
        resource.setStorageType(StorageType.STAGING);

        Storage permanentStorage = new Storage();
        permanentStorage.setBucket("permanent-bucket");
        permanentStorage.setPath("permanent");

        when(resourceRepository.findById(resourceId)).thenReturn(Optional.of(resource));
        when(storageServiceClient.getStorageByType(StorageType.PERMANENT)).thenReturn(permanentStorage);

        AmazonS3Exception s3Exception = new AmazonS3Exception("S3 Error");
        s3Exception.setErrorCode("NoSuchKey");
        when(amazonS3.copyObject(stagingBucketName, originalKey, permanentStorage.getBucket(), newKey)).thenThrow(s3Exception);

        // Test and verify exception
        assertThrows(AudioUploadingException.class, () -> resourceService.moveResourceToPermanent(resourceId));
    }

    //@Test
    void testMoveResourceToPermanent_UnexpectedError() {
        Long resourceId = 1L;
        String originalLocation = "https://staging-bucket.s3.amazonaws.com/files/original-file.mp3";
        String originalKey = "files/original-file.mp3";
        String newKey = "permanent/original-file.mp3";

        Resource resource = new Resource();
        resource.setId(resourceId);
        resource.setLocation(originalLocation);
        resource.setStorageType(StorageType.STAGING);

        Storage permanentStorage = new Storage();
        permanentStorage.setBucket("permanent-bucket");
        permanentStorage.setPath("permanent");
        permanentStorage.setStorageType(StorageType.PERMANENT);

        when(resourceRepository.findById(resourceId)).thenReturn(Optional.of(resource));
        when(storageServiceClient.getStorageByType(StorageType.STAGING)).thenReturn(permanentStorage); // Adjusted to match the actual call
        when(storageServiceClient.getStorageByType(StorageType.PERMANENT)).thenReturn(permanentStorage);

        when(amazonS3.copyObject("staging-bucket", originalKey, "permanent-bucket", newKey))
                .thenThrow(new RuntimeException("Unexpected error"));

        // Test and verify exception
        assertThrows(AudioUploadingException.class, () -> resourceService.moveResourceToPermanent(resourceId));
    }
}

