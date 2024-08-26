package com.krystseu.microservices.resourceprocessor.service;

import com.krystseu.microservices.resourceprocessor.firebase.FirebaseAuthUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
@Slf4j
public class ResourceServiceClient {

    private final WebClient.Builder webClientBuilder;
    private final String resourceServiceEndpoint;
    private final FirebaseAuthUtils firebaseAuthUtils;

    @Autowired
    public ResourceServiceClient(WebClient.Builder webClientBuilder,
                                 @Value("${resource-service.endpoint}") String resourceServiceEndpoint,
                                 FirebaseAuthUtils firebaseAuthUtils) {
        this.webClientBuilder = webClientBuilder;
        this.resourceServiceEndpoint = resourceServiceEndpoint;
        this.firebaseAuthUtils = firebaseAuthUtils;
    }

    public byte[] getResourceData(String resourceId) {
        String url = resourceServiceEndpoint + resourceId;
        String authToken = firebaseAuthUtils.getAuthTokenFromContext();

        return webClientBuilder.build().get()
                .uri(url)
                .header("Authorization", "Bearer " + authToken)
                .retrieve()
                .bodyToMono(byte[].class)
                .block();
    }
}

