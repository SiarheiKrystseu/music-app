package com.krystseu.microservices.resourceprocessor.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class ResourceServiceClient {

    private final WebClient.Builder webClientBuilder;
    private final String resourceServiceEndpoint;

    @Autowired
    public ResourceServiceClient(WebClient.Builder webClientBuilder,
                                 @Value("${resource-service.endpoint}") String resourceServiceEndpoint) {
        this.webClientBuilder = webClientBuilder;
        this.resourceServiceEndpoint = resourceServiceEndpoint;
    }

    public byte[] getResourceData(String resourceId) {
        String url = resourceServiceEndpoint + resourceId;
        return webClientBuilder.build().get()
                .uri(url)
                .retrieve()
                .bodyToMono(byte[].class)
                .block();
    }
}
