package com.krystseu.microservices.resourceprocessor.service.impl;

import com.krystseu.microservices.resourceprocessor.firebase.FirebaseAuthUtils;
import com.krystseu.microservices.resourceprocessor.service.ResourceServiceClient;
import com.krystseu.microservices.resourceprocessor.service.SongServiceClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.util.ReflectionTestUtils;

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

    @Mock
    private FirebaseAuthUtils firebaseAuthUtils;


    @BeforeEach
    void setUp() {
        resourceProcessor = new ResourceProcessorImpl(resourceServiceClient, songServiceClient, rabbitTemplate, firebaseAuthUtils);
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