package com.krystseu.microservices.resourceprocessor.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
@Slf4j
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
        String authToken = getAuthTokenFromContext(); // Get the Authorization token

        return webClientBuilder.build().get()
                .uri(url)
                .header("Authorization", "Bearer " + authToken) // Add Authorization header
                .retrieve()
                .bodyToMono(byte[].class)
                .block();
    }

    private String getAuthTokenFromContext() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getCredentials() != null) {
            return authentication.getCredentials().toString();
        }
        log.warn("Authorization token not found in the security context.");
        return "";
    }
}

