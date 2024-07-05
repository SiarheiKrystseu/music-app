package com.krystseu.microservices.resourceservice.service.impl;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.krystseu.microservices.resourceservice.dto.ResourceResponse;
import com.krystseu.microservices.resourceservice.model.Resource;
import com.krystseu.microservices.resourceservice.repository.ResourceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResourceServiceImplTest {

    @Mock
    private ResourceRepository resourceRepository;

    @InjectMocks
    private ResourceServiceImpl resourceService;

    @Mock
    private AmazonS3 amazonS3;

    private String bucketName = "bucket-name";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(resourceService, "bucketName", bucketName);
    }

    @Test
    void testGetResourceById() throws IOException {
        // Mock data
        Long id = 1L;
        String location = "http://localhost:4510/" + bucketName + "/" + UUID.randomUUID().toString() + ".mp3";
        Resource resource = new Resource();
        resource.setId(id);
        resource.setLocation(location);

        byte[] audioData = "audio data".getBytes();
        S3Object s3Object = mock(S3Object.class);
        S3ObjectInputStream s3ObjectInputStream = new S3ObjectInputStream(new ByteArrayInputStream(audioData.clone()), null);
        when(s3Object.getObjectContent()).thenReturn(s3ObjectInputStream);
        when(resourceRepository.findById(anyLong())).thenReturn(Optional.of(resource));

        // Extract the key using the new method
        String key = resourceService.extractKeyFromLocation(resource.getLocation());

        when(amazonS3.getObject(bucketName, key)).thenReturn(s3Object);

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
        when(resourceRepository.existsById(1L)).thenReturn(true);
        when(resourceRepository.existsById(2L)).thenReturn(true);
        when(resourceRepository.existsById(3L)).thenReturn(true);

        String location1 = UUID.randomUUID().toString() + ".mp3";
        String location2 = UUID.randomUUID().toString() + ".mp3";
        String location3 = UUID.randomUUID().toString() + ".mp3";

        Resource resource1 = new Resource();
        resource1.setId(1L);
        resource1.setLocation(location1);

        Resource resource2 = new Resource();
        resource2.setId(2L);
        resource2.setLocation(location2);

        Resource resource3 = new Resource();
        resource3.setId(3L);
        resource3.setLocation(location3);

        when(resourceRepository.findById(1L)).thenReturn(Optional.of(resource1));
        when(resourceRepository.findById(2L)).thenReturn(Optional.of(resource2));
        when(resourceRepository.findById(3L)).thenReturn(Optional.of(resource3));

        doNothing().when(amazonS3).deleteObject(bucketName, location1);
        doNothing().when(amazonS3).deleteObject(bucketName, location2);
        doNothing().when(amazonS3).deleteObject(bucketName, location3);

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
}

