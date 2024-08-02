package com.krystseu.microservices.resourceservice.contract;

import com.amazonaws.services.s3.model.PutObjectResult;
import com.amazonaws.services.s3.model.S3Object;
import com.krystseu.microservices.resourceservice.model.Resource;
import com.krystseu.microservices.resourceservice.repository.ResourceRepository;
import com.krystseu.microservices.resourceservice.service.ResourceMessageSender;
import com.krystseu.microservices.resourceservice.service.StorageServiceClient;
import com.krystseu.microservices.resourceservice.service.impl.ResourceServiceImpl;
import com.krystseu.microservices.storageservice.model.Storage;
import com.krystseu.microservices.storageservice.model.StorageType;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.io.ByteArrayInputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.PutObjectRequest;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
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

    private ResourceServiceImpl resourceService;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
        resourceService = new ResourceServiceImpl(
                resourceRepository, amazonS3, rabbitTemplate, storageServiceClient,
                queueName, resourceAckQueueName);

        // Spy on resourceService to stub processResourceAsync method
        resourceService = Mockito.spy(resourceService);
        doNothing().when(resourceService).processResourceAsync(any(Resource.class));
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

        // Mock behavior for S3 client
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

        // Mock RabbitTemplate behavior
        when(rabbitTemplate.receive(eq(resourceAckQueueName), anyLong())).thenAnswer(invocation -> {
            MessageProperties messageProperties = new MessageProperties();
            messageProperties.setCorrelationId("ac1b9b35-5913-4048-944d-eff00d788220");
            return new Message("1".getBytes(StandardCharsets.UTF_8), messageProperties);
        });

        // When
        MvcResult result = mockMvc.perform(MockMvcRequestBuilders
                        .post("/api/resources/upload")
                        .content(fileContent)
                        .contentType("audio/mpeg"))
                .andExpect(MockMvcResultMatchers.status().isCreated())
                .andReturn();

        // Then
        assertThat(result.getResponse().getStatus()).isEqualTo(201);
    }
}
