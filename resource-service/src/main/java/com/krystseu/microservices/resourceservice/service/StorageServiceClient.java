package com.krystseu.microservices.resourceservice.service;

import com.krystseu.microservices.resourceservice.exception.StorageConfigurationException;
import com.krystseu.microservices.storageservice.model.Storage;
import com.krystseu.microservices.storageservice.model.StorageType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class StorageServiceClient {

    private final WebClient.Builder webClientBuilder;
    private final String storageServiceEndpoint;
    private final Map<StorageType, Storage> storageMap = new ConcurrentHashMap<>();

    @Autowired
    public StorageServiceClient(WebClient.Builder webClientBuilder,
                                @Value("${storage-service.endpoint}") String storageServiceEndpoint) {
        this.webClientBuilder = webClientBuilder;
        this.storageServiceEndpoint = storageServiceEndpoint;
    }

    public Storage getStorageByType(StorageType storageType) {
        Storage storage = storageMap.get(storageType);
        if (storage == null) {
            log.info("Storage configuration not found for type: {}, fetching configurations", storageType);
            fetchAndBlockStorageConfigurations();
            storage = storageMap.get(storageType);
            if (storage == null) {
                log.error("Storage configuration still not available for type: {}", storageType);
                throw new StorageConfigurationException("Storage configuration not available for type: " + storageType);
            }
        }
        return storage;
    }

    private void fetchAndBlockStorageConfigurations() {
        Map<StorageType, Storage> configurations = fetchStorageConfigurations().block();
        if (configurations != null) {
            updateStorageMap(configurations);
        } else {
            log.error("Failed to fetch storage configurations synchronously");
            throw new StorageConfigurationException("Failed to fetch storage configurations");
        }
    }


    private Mono<Map<StorageType, Storage>> fetchStorageConfigurations() {
        return webClientBuilder.build().get()
                .uri(storageServiceEndpoint)
                .retrieve()
                .bodyToFlux(Storage.class)
                .collectMap(Storage::getStorageType)
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(5)))
                .doOnSuccess(map -> log.info("Successfully fetched storage configurations"))
                .doOnError(error -> log.error("Failed to fetch storage configurations", error));
    }

    private void updateStorageMap(Map<StorageType, Storage> newMap) {
        storageMap.clear();
        storageMap.putAll(newMap);
        log.info("Updated storage configurations. Current storage types and configurations:");
        newMap.forEach((type, storage) -> log.info("Storage Type: {}, Configuration: {}", type, storage));
    }

    private void handleError(Throwable throwable) {
        log.error("Failed to fetch storage configurations", throwable);
    }
}

