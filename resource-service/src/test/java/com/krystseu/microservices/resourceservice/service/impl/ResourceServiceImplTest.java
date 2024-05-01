package com.krystseu.microservices.resourceservice.service.impl;

import com.krystseu.microservices.resourceservice.dto.ResourceResponse;
import com.krystseu.microservices.resourceservice.model.Resource;
import com.krystseu.microservices.resourceservice.repository.ResourceRepository;
import com.krystseu.microservices.songservice.dto.SongResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ResourceServiceImplTest {

    @Mock
    private ResourceRepository resourceRepository;

    @InjectMocks
    private ResourceServiceImpl resourceService;

    @Mock
    private WebClient.Builder webClientBuilder;

    @Mock
    private WebClient webClient;

    @Mock
    private WebClient.RequestHeadersUriSpec requestHeadersUriSpec;

    @Mock
    private WebClient.RequestBodySpec requestBodySpec;

    @Mock
    private WebClient.RequestHeadersSpec requestHeadersSpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);

        WebClient.RequestBodyUriSpec requestBodyUriSpec = mock(WebClient.RequestBodyUriSpec.class);
        when(webClientBuilder.build()).thenReturn(webClient);

        // Mock WebClient behavior for GET
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.exchangeToMono(any())).thenReturn(Mono.just(HttpStatus.OK)); // Add this line
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(any(Class.class))).thenReturn(Mono.just(new SongResponse()));

        // Mock WebClient behavior for POST
        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(any(MediaType.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.body(any(Mono.class), any(Class.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(any(Class.class))).thenReturn(Mono.just(new SongResponse()));
    }

    @Test
    void testGetResourceById() {
        // Mock data
        Long id = 1L;
        Resource resource = new Resource();
        resource.setId(id);
        when(resourceRepository.findById(id)).thenReturn(Optional.of(resource));

        // Test
        Optional<ResourceResponse> resourceResponseOptional = resourceService.getResourceById(id.intValue());

        // Assertions
        assertTrue(resourceResponseOptional.isPresent());
        assertEquals(id, resourceResponseOptional.get().getId());
    }

    @Test
    void testDeleteResources() {
        // Mock data
        String idsCSV = "1,2,3";
        when(resourceRepository.existsById(1L)).thenReturn(true);
        when(resourceRepository.existsById(2L)).thenReturn(true);
        when(resourceRepository.existsById(3L)).thenReturn(true);
        doNothing().when(resourceRepository).deleteById(1L);
        doNothing().when(resourceRepository).deleteById(2L);
        doNothing().when(resourceRepository).deleteById(3L);

        // Test
        List<Integer> deletedIds = resourceService.deleteResources(idsCSV);

        // Assertions
        assertNotNull(deletedIds);
        assertEquals(3, deletedIds.size());
        assertTrue(deletedIds.containsAll(List.of(1,2,3)));
    }
}