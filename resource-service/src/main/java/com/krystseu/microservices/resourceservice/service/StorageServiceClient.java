package com.krystseu.microservices.resourceservice.service;

import com.krystseu.microservices.resourceservice.exception.StorageConfigurationException;
import com.krystseu.microservices.resourceservice.firebase.FirebaseAuthentication;
import com.krystseu.microservices.storageservice.model.Storage;
import com.krystseu.microservices.storageservice.model.StorageType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import java.util.Map;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class StorageServiceClient {

    private final WebClient.Builder webClientBuilder;
    private final String storageServiceEndpoint;
    private final Map<StorageType, Storage> stubStorageConfigurations = new ConcurrentHashMap<>();

    @Autowired
    public StorageServiceClient(WebClient.Builder webClientBuilder,
                                @Value("${storage-service.endpoint}") String storageServiceEndpoint) {
        this.webClientBuilder = webClientBuilder;
        this.storageServiceEndpoint = storageServiceEndpoint;

        // Initialize stub storage configurations
        this.stubStorageConfigurations.put(StorageType.STAGING, Storage.builder()
                .storageType(StorageType.STAGING)
                .bucket("stub-staging-bucket")
                .path("stub/staging/path")
                .build());
        this.stubStorageConfigurations.put(StorageType.PERMANENT, Storage.builder()
                .storageType(StorageType.PERMANENT)
                .bucket("stub-permanent-bucket")
                .path("stub/permanent/path")
                .build());

        log.info("StorageServiceClient initialized with stub configurations");
    }

    @CircuitBreaker(name = "storageServiceCircuitBreaker", fallbackMethod = "fallbackGetStorageByType")
    public Storage getStorageByType(StorageType storageType) {
        log.info("Fetching storage configuration for type: {}", storageType);
        return fetchStorageConfigurations()
                .blockOptional()
                .map(configurations -> configurations.get(storageType))
                .orElseThrow(() -> {
                    log.error("Storage configuration not available for type: {}", storageType);
                    return new StorageConfigurationException("Storage configuration not available for type: " + storageType);
                });
    }

    private Mono<Map<StorageType, Storage>> fetchStorageConfigurations() {
        String authToken = getAuthTokenFromContext(); // Get the Authorization token

        return webClientBuilder.build().get()
                .uri(storageServiceEndpoint)
                .header("Authorization", "Bearer " + authToken) // Add Authorization header
                .retrieve()
                .bodyToFlux(Storage.class)
                .collectMap(Storage::getStorageType)
                .doOnSuccess(map -> log.info("Successfully fetched storage configurations"))
                .doOnError(error -> log.error("Failed to fetch storage configurations", error));
    }

    private String getAuthTokenFromContext() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof FirebaseAuthentication firebaseAuth) {
            log.info("Retrieving auth token: {}", firebaseAuth.getRawToken());
            return firebaseAuth.getRawToken();
        }
        log.warn("Authorization token not found in the security context.");
        return "";
    }

    public Storage fallbackGetStorageByType(StorageType storageType, Throwable throwable) {
        log.warn("Fallback method invoked for storage type: {}. Reason: {}", storageType, throwable.getMessage());
        return stubStorageConfigurations.get(storageType);
    }
}