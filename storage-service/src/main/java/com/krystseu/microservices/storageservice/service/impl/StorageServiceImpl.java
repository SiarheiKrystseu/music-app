package com.krystseu.microservices.storageservice.service.impl;

import com.krystseu.microservices.storageservice.exception.InvalidStorageException;
import com.krystseu.microservices.storageservice.exception.StorageNotFoundException;
import com.krystseu.microservices.storageservice.model.Storage;
import com.krystseu.microservices.storageservice.model.StorageType;
import com.krystseu.microservices.storageservice.repository.StorageRepository;
import com.krystseu.microservices.storageservice.service.StorageService;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
@Slf4j
public class StorageServiceImpl implements StorageService {

    private final StorageRepository storageRepository;

    @Autowired
    public StorageServiceImpl(StorageRepository storageRepository) {
        this.storageRepository = storageRepository;
    }

    @Override
    public Storage createStorage(Storage storage) {
        log.info("Creating storage with type {}", storage.getStorageType());

        validateStorage(storage);

        Storage savedStorage = storageRepository.save(storage);
        log.info("Created storage with ID {}", savedStorage.getId());
        return savedStorage;
    }

    @Override
    public List<Storage> getAllStorages() {
        log.info("Retrieving all storages");
        List<Storage> storages = storageRepository.findAll();
        log.info("Retrieved {} storages", storages.size());
        return storages;
    }

    @Override
    public List<Long> deleteStorages(String idsCSV) {
        List<Long> ids = Arrays.stream(idsCSV.split(","))
                .map(Long::parseLong)
                .collect(Collectors.toList());
        log.info("Deleting storages with IDs {}", ids);
        storageRepository.deleteAllById(ids);
        log.info("Deleted storages with IDs {}", ids);
        return ids;
    }

    @Override
    public Optional<Storage> getStorageById(Long id) {
        log.info("Retrieving storage with ID {}", id);
        Optional<Storage> storageOptional = storageRepository.findById(id);
        if (storageOptional.isPresent()) {
            log.info("Found storage with ID {}", id);
        } else {
            log.warn("Storage with ID {} does not exist", id);
        }
        return storageOptional;
    }

    @Override
    public Storage getStorageByType(StorageType storageType) {
        log.info("Retrieving storage with type {}", storageType);
        Optional<Storage> storageOptional = storageRepository.findByStorageType(storageType);
        if (storageOptional.isPresent()) {
            log.info("Found storage with type {}", storageType);
        } else {
            log.warn("Storage with type {} does not exist", storageType);
            throw new StorageNotFoundException("The storage with the specified type does not exist");
        }
        return storageOptional.get();
    }
    private void validateStorage(Storage storage) {
        // Check if storageType is valid
        if (storage.getStorageType() == null) {
            throw new InvalidStorageException("Invalid storageType. It must not be null.");
        }
        // Check if storageType is STAGING or PERMANENT
        if (!EnumSet.of(StorageType.STAGING, StorageType.PERMANENT).contains(storage.getStorageType())) {
            throw new InvalidStorageException("Invalid storageType. It must be either 'STAGING' or 'PERMANENT'.");
        }
        // Check if bucket is not null or empty
        if (storage.getBucket() == null || storage.getBucket().trim().isEmpty()) {
            throw new InvalidStorageException("Invalid bucket. It must not be null or empty.");
        }
        // Check if path is not null or empty
        if (storage.getPath() == null || storage.getPath().trim().isEmpty()) {
            throw new InvalidStorageException("Invalid path. It must not be null or empty.");
        }
    }
}
