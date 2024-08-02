package com.krystseu.microservices.resourceprocessor.service.impl;

import com.krystseu.microservices.resourceprocessor.exception.FileParsingException;
import com.krystseu.microservices.resourceprocessor.service.ResourceServiceClient;
import com.krystseu.microservices.resourceprocessor.service.SongServiceClient;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResourceProcessorImplTest {

    @Mock
    private ResourceServiceClient resourceServiceClient;

    @Mock
    private SongServiceClient songServiceClient;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private ResourceProcessorImpl resourceProcessor;

    @BeforeEach
    void setUp() {
        resourceProcessor = new ResourceProcessorImpl(resourceServiceClient, songServiceClient, rabbitTemplate);
        ReflectionTestUtils.setField(resourceProcessor, "resourceAckQueue", "ackQueue");
    }

    /*@Test
    void processResourceMessageTest() throws IOException, TikaException {
        // Given
        String resourceId = "1";
        byte[] resourceData = Files.readAllBytes(Paths.get("src/test/resources/test.mp3"));

        // Mocking the external service calls
        when(resourceServiceClient.getResourceData(resourceId)).thenReturn(resourceData);
        doNothing().when(songServiceClient).saveMetadata(any(Metadata.class), eq(Long.valueOf(resourceId)));

        // Setup and mocking
        doNothing().when(rabbitTemplate).convertAndSend(eq("ackQueue"), eq("1"));

        // When
        resourceProcessor.processResourceMessage(resourceId);  // Ensure this is called only once

        // Then
        verify(resourceServiceClient).getResourceData(resourceId);
        verify(songServiceClient).saveMetadata(any(Metadata.class), eq(Long.valueOf(resourceId)));
        verify(rabbitTemplate).convertAndSend(eq("ackQueue"), eq("1"));
    }



    @Test
    void processResourceMessageWhenExceptionThrownTest() {
        // Given
        String resourceId = "1";
        when(resourceServiceClient.getResourceData(resourceId)).thenReturn(null);

        // When & Then
        assertThrows(FileParsingException.class, () -> resourceProcessor.processResourceMessage(resourceId));
    }*/
}