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
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
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

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private ResourceServiceImpl resourceService;

    private Storage stagingStorage;
    private Storage permanentStorage;

    @BeforeEach
    void setUp() {
        // Initialize the storage objects before each test
        stagingStorage = new Storage();
        stagingStorage.setBucket("staging-bucket");
        stagingStorage.setPath("files");

        permanentStorage = new Storage();
        permanentStorage.setBucket("permanent-bucket");
        permanentStorage.setPath("files");
    }

    @Test
    void testGetResourceById() throws IOException {
        // Mock data
        Long id = 1L;
        String location = "http://localhost:4510/permanent-bucket/files/" + UUID.randomUUID().toString() + ".mp3";
        Resource resource = new Resource();
        resource.setId(id);
        resource.setLocation(location);
        resource.setStorageType(StorageType.PERMANENT);

        byte[] audioData = "audio data".getBytes();
        S3Object s3Object = mock(S3Object.class);
        S3ObjectInputStream s3ObjectInputStream = new S3ObjectInputStream(new ByteArrayInputStream(audioData.clone()), null);
        when(s3Object.getObjectContent()).thenReturn(s3ObjectInputStream);
        when(resourceRepository.findById(anyLong())).thenReturn(Optional.of(resource));

        when(storageServiceClient.getStorageByType(StorageType.PERMANENT)).thenReturn(permanentStorage);

        String key = resourceService.extractKeyFromLocation(resource.getLocation(), permanentStorage.getPath());
        when(amazonS3.getObject(permanentStorage.getBucket(), key)).thenReturn(s3Object);

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

        Resource resource1 = new Resource();
        resource1.setId(1L);
        resource1.setLocation("http://localhost:4510/permanent-bucket/files/1.mp3");
        resource1.setStorageType(StorageType.PERMANENT);

        Resource resource2 = new Resource();
        resource2.setId(2L);
        resource2.setLocation("http://localhost:4510/permanent-bucket/files/2.mp3");
        resource2.setStorageType(StorageType.PERMANENT);

        Resource resource3 = new Resource();
        resource3.setId(3L);
        resource3.setLocation("http://localhost:4510/permanent-bucket/files/3.mp3");
        resource3.setStorageType(StorageType.PERMANENT);

        when(resourceRepository.findById(1L)).thenReturn(Optional.of(resource1));
        when(resourceRepository.findById(2L)).thenReturn(Optional.of(resource2));
        when(resourceRepository.findById(3L)).thenReturn(Optional.of(resource3));

        when(storageServiceClient.getStorageByType(StorageType.PERMANENT)).thenReturn(permanentStorage);

        doNothing().when(amazonS3).deleteObject(eq(permanentStorage.getBucket()), anyString());
        doNothing().when(resourceRepository).deleteById(anyLong());

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

        // Additional test cases to check for various input conditions
    }

    @Test
    void testMoveResourceToPermanent() throws Exception {
        // Setup mock data
        Long resourceId = 1L;
        String originalLocation = "https://staging-bucket.s3.amazonaws.com/files/original-file.mp3";
        String newLocation = "https://permanent-bucket.s3.amazonaws.com/files/original-file.mp3";

        Resource resource = new Resource();
        resource.setId(resourceId);
        resource.setLocation(originalLocation);
        resource.setStorageType(StorageType.STAGING);

        String originalKey = "files/original-file.mp3";
        String newKey = "files/original-file.mp3";

        // Mocking dependencies
        when(resourceRepository.findById(resourceId)).thenReturn(Optional.of(resource));
        when(storageServiceClient.getStorageByType(StorageType.STAGING)).thenReturn(stagingStorage);
        when(storageServiceClient.getStorageByType(StorageType.PERMANENT)).thenReturn(permanentStorage);
        when(amazonS3.copyObject(stagingStorage.getBucket(), originalKey, permanentStorage.getBucket(), newKey))
                .thenReturn(new CopyObjectResult());
        when(amazonS3.getUrl(permanentStorage.getBucket(), newKey)).thenReturn(new URL(newLocation));

        // Act
        resourceService.moveResourceToPermanent(resourceId);

        // Verify interactions with mocks
        verify(amazonS3).copyObject(stagingStorage.getBucket(), originalKey, permanentStorage.getBucket(), newKey);
        verify(amazonS3).deleteObject(stagingStorage.getBucket(), originalKey);

        // Capture and verify the saved resource
        ArgumentCaptor<Resource> resourceCaptor = ArgumentCaptor.forClass(Resource.class);
        verify(resourceRepository).save(resourceCaptor.capture());
        Resource savedResource = resourceCaptor.getValue();

        assertEquals(newLocation, savedResource.getLocation());
        assertEquals(StorageType.PERMANENT, savedResource.getStorageType());
    }


    @Test
    void testMoveResourceToPermanent_ResourceNotFound() {
        // Arrange
        Long resourceId = 100L;

        // Mock the behavior of resourceRepository to throw ResourceNotFoundException
        when(resourceRepository.findById(resourceId)).thenThrow(new ResourceNotFoundException("Resource not found with ID: " + resourceId));

        // Act & Assert: Verify that AudioUploadingException is thrown
        AudioUploadingException thrownException = assertThrows(AudioUploadingException.class, () -> {
            resourceService.moveResourceToPermanent(resourceId);
        });

        assertEquals("Failed to move resource to permanent storage", thrownException.getMessage());
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

        when(resourceRepository.findById(resourceId)).thenReturn(Optional.of(resource));
        when(storageServiceClient.getStorageByType(StorageType.STAGING)).thenReturn(stagingStorage);
        when(storageServiceClient.getStorageByType(StorageType.PERMANENT)).thenReturn(permanentStorage);

        AmazonS3Exception s3Exception = new AmazonS3Exception("S3 Error");
        s3Exception.setErrorCode("NoSuchKey");
        when(amazonS3.copyObject(stagingStorage.getBucket(), originalKey, permanentStorage.getBucket(), newKey)).thenThrow(s3Exception);

        // Test and verify exception
        assertThrows(AudioUploadingException.class, () -> resourceService.moveResourceToPermanent(resourceId));
    }

    @Test
    void testMoveResourceToPermanent_UnexpectedError() {
        Long resourceId = 1L;
        String originalLocation = "https://staging-bucket.s3.amazonaws.com/files/original-file.mp3";
        String originalKey = "files/original-file.mp3";
        String newKey = "files/original-file.mp3";

        Resource resource = new Resource();
        resource.setId(resourceId);
        resource.setLocation(originalLocation);
        resource.setStorageType(StorageType.STAGING);

        when(resourceRepository.findById(resourceId)).thenReturn(Optional.of(resource));
        when(storageServiceClient.getStorageByType(StorageType.STAGING)).thenReturn(stagingStorage);
        when(storageServiceClient.getStorageByType(StorageType.PERMANENT)).thenReturn(permanentStorage);

        when(amazonS3.copyObject(eq(stagingStorage.getBucket()), eq(originalKey), eq(permanentStorage.getBucket()), eq(newKey)))
                .thenThrow(new RuntimeException("Unexpected error"));

        assertThrows(AudioUploadingException.class, () -> resourceService.moveResourceToPermanent(resourceId));
    }
}



