package com.krystseu.microservices.resourceservice.controller;

import com.krystseu.microservices.resourceservice.dto.ResourceResponse;
import com.krystseu.microservices.resourceservice.model.Resource;
import com.krystseu.microservices.resourceservice.service.ResourceService;
import com.krystseu.microservices.songservice.dto.SongResponse;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.web.reactive.function.client.WebClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.mockito.MockitoAnnotations;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class ResourceControllerTest {
    @Mock
    private WebClient webClient;

    @Mock
    private WebClient.RequestHeadersUriSpec requestHeadersUriSpec;

    @Mock
    private WebClient.RequestHeadersSpec requestHeadersSpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);

        // Mock WebClient behavior
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(any(Class.class))).thenReturn(Mono.just(new SongResponse()));
        postgreSQLContainer.start();
    }
    @Container
    static PostgreSQLContainer postgreSQLContainer = new PostgreSQLContainer<>("postgres:16.2")
            .withDatabaseName("test")
            .withUsername("test")
            .withPassword("test")
            .withInitScript("init.sql");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ResourceService resourceService;

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry dynamicPropertyRegistry) {
        dynamicPropertyRegistry.add("spring.datasource.url", postgreSQLContainer::getJdbcUrl);
        dynamicPropertyRegistry.add("spring.datasource.username", postgreSQLContainer::getUsername);
        dynamicPropertyRegistry.add("spring.datasource.password", postgreSQLContainer::getPassword);
    }


    @Test
    void testDeleteResources() throws Exception {
        // Set up data
        resourceService.uploadFile(new MockMultipartFile("file", "test.mp3", "audio/mpeg", "test data".getBytes()));
        resourceService.uploadFile(new MockMultipartFile("file", "test.mp3", "audio/mpeg", "test data".getBytes()));
        resourceService.uploadFile(new MockMultipartFile("file", "test.mp3", "audio/mpeg", "test data".getBytes()));

        mockMvc.perform(MockMvcRequestBuilders.delete("/api/resources?id=1,2,3"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.ids", Matchers.containsInAnyOrder(1, 2, 3)));
    }

    @Test
    void testUploadFile() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "test.mp3", "audio/mpeg", "test data".getBytes());
        mockMvc.perform(MockMvcRequestBuilders.multipart("/api/resources/upload").file(file))
                .andExpect(MockMvcResultMatchers.status().isCreated());
    }
}