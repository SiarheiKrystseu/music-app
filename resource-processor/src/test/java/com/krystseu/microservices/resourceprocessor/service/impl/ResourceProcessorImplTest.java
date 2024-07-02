package com.krystseu.microservices.resourceprocessor.service.impl;

import com.krystseu.microservices.resourceprocessor.exception.FileParsingException;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class ResourceProcessorImplTest {

    @InjectMocks
    private ResourceProcessorImpl resourceProcessor;

    @Test
    void processResourceTest() throws IOException, TikaException {
        byte[] resourceData = Files.readAllBytes(Paths.get("src/test/resources/test.mp3"));
        Metadata result = resourceProcessor.processResource(resourceData);
        assertEquals("MP3", result.get("xmpDM:audioCompressor"));
        assertEquals("audio/mpeg", result.get("Content-Type"));
    }

    @Test
    void processResourceWhenExceptionThrownTest() {
        byte[] resourceData = null;
        assertThrows(FileParsingException.class, () -> resourceProcessor.processResource(resourceData));
    }
}