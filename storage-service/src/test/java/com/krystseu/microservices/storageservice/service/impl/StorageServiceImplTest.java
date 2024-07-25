package com.krystseu.microservices.storageservice.service.impl;

import com.krystseu.microservices.storageservice.exception.InvalidStorageException;
import com.krystseu.microservices.storageservice.model.Storage;
import com.krystseu.microservices.storageservice.model.StorageType;
import com.krystseu.microservices.storageservice.model.repository.StorageRepository;
import com.krystseu.microservices.storageservice.model.service.impl.StorageServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StorageServiceImplTest {

    @Mock
    private StorageRepository storageRepository;

    @InjectMocks
    private StorageServiceImpl storageService;

    @Test
    void testCreateStorage() {
        // Mock data
        Storage storage = new Storage();
        storage.setStorageType(StorageType.STAGING);
        storage.setBucket("bucket");
        storage.setPath("/path");

        when(storageRepository.save(any(Storage.class))).thenReturn(storage);

        // Test
        Storage createdStorage = storageService.createStorage(storage);

        // Assertions
        assertNotNull(createdStorage);
        assertEquals(StorageType.STAGING, createdStorage.getStorageType());
    }

    @Test
    void testCreateStorageInvalidStorageType() {
        // Mock data
        Storage storage = new Storage();
        storage.setStorageType(null);
        storage.setBucket("bucket");
        storage.setPath("/path");

        // Test and assert exception
        assertThrows(InvalidStorageException.class, () -> storageService.createStorage(storage));
    }

    @Test
    void testGetAllStorages() {
        // Mock data
        Storage storage1 = new Storage();
        storage1.setStorageType(StorageType.STAGING);

        Storage storage2 = new Storage();
        storage2.setStorageType(StorageType.PERMANENT);

        List<Storage> storages = Arrays.asList(storage1, storage2);

        when(storageRepository.findAll()).thenReturn(storages);

        // Test
        List<Storage> retrievedStorages = storageService.getAllStorages();

        // Assertions
        assertNotNull(retrievedStorages);
        assertEquals(2, retrievedStorages.size());
        assertEquals(StorageType.STAGING, retrievedStorages.get(0).getStorageType());
        assertEquals(StorageType.PERMANENT, retrievedStorages.get(1).getStorageType());
    }

    @Test
    void testDeleteStorages() {
        // Mock data
        String idsCSV = "1,2,3";
        List<Long> ids = Arrays.asList(1L, 2L, 3L);

        doNothing().when(storageRepository).deleteAllById(ids);

        // Test
        List<Long> deletedIds = storageService.deleteStorages(idsCSV);

        // Assertions
        assertNotNull(deletedIds);
        assertEquals(ids, deletedIds);

        // Verify that the method was called
        verify(storageRepository, times(1)).deleteAllById(ids);
    }
}
