package com.krystseu.microservices.resourceprocessor.service.integration;

import com.krystseu.microservices.resourceprocessor.service.ResourceProcessor;
import com.krystseu.microservices.resourceprocessor.service.ResourceServiceClient;
import com.krystseu.microservices.resourceprocessor.service.SongServiceClient;
import com.krystseu.microservices.resourceprocessor.service.config.TestRabbitMqConfig;
import org.apache.tika.metadata.Metadata;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@Import(TestRabbitMqConfig.class)
public class ResourceProcessorIntegrationTest {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @MockBean
    private ResourceServiceClient resourceServiceClient;

    @MockBean
    private SongServiceClient songServiceClient;

    @Autowired
    private ResourceProcessor resourceProcessor;

    @Value("${resource.rabbitmq.queue}")
    private String rabbitMqQueue;

    @Test
    void testProcessResourceMessage() throws Exception {
        // Given
        String resourceId = "1";
        byte[] mockResourceData = "mock data".getBytes();

        // Mock interactions
        when(resourceServiceClient.getResourceData(resourceId)).thenReturn(mockResourceData);
        doNothing().when(songServiceClient).saveMetadata(any(Metadata.class), any(Long.class));
        // Send a message to the queue
        rabbitTemplate.convertAndSend(rabbitMqQueue, resourceId);

        // Wait for the message to be processed
        Thread.sleep(1000);

        // Verify interactions
        verify(resourceServiceClient).getResourceData(resourceId);
        verify(songServiceClient).saveMetadata(any(Metadata.class), any(Long.class));
    }
}