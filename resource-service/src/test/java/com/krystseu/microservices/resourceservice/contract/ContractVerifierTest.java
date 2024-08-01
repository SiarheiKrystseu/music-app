package com.krystseu.microservices.resourceservice.contract;

import com.amazonaws.services.s3.model.PutObjectResult;
import com.amazonaws.services.s3.model.S3Object;
import com.krystseu.microservices.resourceservice.config.TestLocalStackConfig;
import com.krystseu.microservices.resourceservice.config.TestRabbitMqConfig;
import com.krystseu.microservices.resourceservice.model.Resource;
import com.krystseu.microservices.resourceservice.repository.ResourceRepository;
import com.krystseu.microservices.resourceservice.service.ResourceMessageSender;
import com.krystseu.microservices.resourceservice.service.StorageServiceClient;
import com.krystseu.microservices.storageservice.model.Storage;
import com.krystseu.microservices.storageservice.model.StorageType;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;


@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
@ContextConfiguration(classes = {TestLocalStackConfig.class, TestRabbitMqConfig.class})
@ActiveProfiles("test")
public class ContractVerifierTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ResourceRepository resourceRepository;

    @MockBean
    private AmazonS3 amazonS3;

    @MockBean
    private ResourceMessageSender resourceMessageSender;

    @MockBean
    private ApplicationEventPublisher eventPublisher;

    @MockBean
    private StorageServiceClient storageServiceClient;

    @MockBean
    private RabbitTemplate rabbitTemplate;

    @Value("${cloud.aws.s3.staging-bucket-name}")
    private String stagingBucketName;

    @Value("${cloud.aws.s3.permanent-bucket-name}")
    private String permanentBucketName;

    @Value("${resource.rabbitmq.queue}")
    private String queueName;

    @Value("${resource.ack.queue}")
    private String resourceAckQueueName;

    @BeforeEach
    public void setup() {
        // Configure your mocks here if necessary
    }

    @Test
    void validate_httpContract() throws Exception {
        // Given
        byte[] fileContent = Files.readAllBytes(Paths.get("src/test/resources/Test_Audio.mp3"));

        // Mock behavior for storage service client
        Storage stagingStorage = new Storage();
        stagingStorage.setBucket(stagingBucketName);
        stagingStorage.setPath("files");

        Storage permanentStorage = new Storage();
        permanentStorage.setBucket(permanentBucketName);
        permanentStorage.setPath("files");

        when(storageServiceClient.getStorageByType(StorageType.STAGING)).thenReturn(stagingStorage);
        when(storageServiceClient.getStorageByType(StorageType.PERMANENT)).thenReturn(permanentStorage);

        // Create and configure S3Object
        S3Object s3Object = new S3Object();
        s3Object.setObjectContent(new ByteArrayInputStream(fileContent));
        when(amazonS3.getObject(eq(stagingBucketName), anyString())).thenReturn(s3Object);
        when(amazonS3.getObject(eq(permanentBucketName), anyString())).thenReturn(s3Object);

        // Mock behavior for S3 client
        when(amazonS3.putObject(any(PutObjectRequest.class))).thenReturn(new PutObjectResult());
        when(amazonS3.getUrl(anyString(), anyString())).thenReturn(new URL("http://example.com/file.mp3"));

        // Mock behavior for resource repository
        Resource stagedResource = new Resource();
        stagedResource.setId(1L);
        stagedResource.setStorageType(StorageType.STAGING);
        stagedResource.setLocation("http://example.com/staging/files/file.mp3");

        Resource permanentResource = new Resource();
        permanentResource.setId(1L);
        permanentResource.setStorageType(StorageType.PERMANENT);
        permanentResource.setLocation("http://example.com/permanent/files/file.mp3");

        when(resourceRepository.save(any(Resource.class))).thenReturn(stagedResource);
        when(resourceRepository.findById(anyLong())).thenReturn(Optional.of(stagedResource)).thenReturn(Optional.of(permanentResource));

        // Capture the correlationId
        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);

        // Mock RabbitTemplate behavior
        when(rabbitTemplate.receive(resourceAckQueueName, 1000)).thenAnswer(invocation -> {
            MessageProperties messageProperties = new MessageProperties();
            String correlationId = "ac1b9b35-5913-4048-944d-eff00d788220"; // Simulate generated correlationId
            messageProperties.setCorrelationId(correlationId);
            return new Message("1".getBytes(StandardCharsets.UTF_8), messageProperties);
        });

        // Mock getResource method
        when(resourceRepository.findById(1L)).thenReturn(Optional.of(stagedResource));

        // When
        MvcResult result = mockMvc.perform(MockMvcRequestBuilders
                        .post("/api/resources/upload")
                        .content(fileContent)
                        .contentType("audio/mpeg"))
                .andExpect(MockMvcResultMatchers.status().isCreated())
                .andReturn();

        // Then
        assertThat(result.getResponse().getStatus()).isEqualTo(201);

        // Verify that receive method was called with correct queue and timeout
        verify(rabbitTemplate, times(1)).receive(eq(resourceAckQueueName), eq(1000));

        // Ensure correlationId matches the expected value
        verify(rabbitTemplate).receive(eq(resourceAckQueueName), eq(1000));
        Message capturedMessage = messageCaptor.getValue();
        assertThat(capturedMessage.getMessageProperties().getCorrelationId()).isEqualTo("ac1b9b35-5913-4048-944d-eff00d788220");
    }
}
