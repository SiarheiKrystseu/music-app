package com.krystseu.microservices.storageservice.service;

import com.krystseu.microservices.storageservice.model.Storage;
import com.krystseu.microservices.storageservice.model.StorageType;

import java.util.List;
import java.util.Optional;

public interface StorageService {
    Storage createStorage(Storage storage);
    List<Storage> getAllStorages();
    List<Long> deleteStorages(String idsCSV);
    Optional<Storage> getStorageById(Long id);
    Storage getStorageByType(StorageType storageType);
}