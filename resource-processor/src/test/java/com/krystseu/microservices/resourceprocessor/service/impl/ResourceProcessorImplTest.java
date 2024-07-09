package com.krystseu.microservices.resourceprocessor.service.impl;

import com.krystseu.microservices.resourceprocessor.exception.FileParsingException;
import com.krystseu.microservices.resourceprocessor.service.ResourceServiceClient;
import com.krystseu.microservices.resourceprocessor.service.SongServiceClient;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResourceProcessorImplTest {

    @Mock
    private ResourceServiceClient resourceServiceClient;

    @Mock
    private SongServiceClient songServiceClient;

    @InjectMocks
    private ResourceProcessorImpl resourceProcessor;

    @Test
    void processResourceMessageTest() throws IOException, TikaException {
        // Given
        String resourceId = "1";
        byte[] resourceData = Files.readAllBytes(Paths.get("src/test/resources/test.mp3"));
        when(resourceServiceClient.getResourceData(resourceId)).thenReturn(resourceData);

        // When
        resourceProcessor.processResourceMessage(resourceId);

        // Then
        verify(resourceServiceClient).getResourceData(resourceId);
        verify(songServiceClient).saveMetadata(any(Metadata.class), eq(Long.valueOf(resourceId)));
    }

    @Test
    void processResourceMessageWhenExceptionThrownTest() {
        // Given
        String resourceId = "1";
        when(resourceServiceClient.getResourceData(resourceId)).thenReturn(null);

        // When & Then
        assertThrows(FileParsingException.class, () -> resourceProcessor.processResourceMessage(resourceId));
    }
}