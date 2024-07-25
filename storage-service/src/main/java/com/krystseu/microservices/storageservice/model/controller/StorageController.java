package com.krystseu.microservices.storageservice.model.controller;

import com.krystseu.microservices.storageservice.exception.StorageNotFoundException;
import com.krystseu.microservices.storageservice.model.Storage;
import com.krystseu.microservices.storageservice.model.service.StorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.InvalidParameterException;
import java.util.*;

@RestController
@RequestMapping("/api/storages")
public class StorageController {

    private final StorageService storageService;

    @Autowired
    public StorageController(StorageService storageService) {
        this.storageService = storageService;
    }

    @PostMapping
    public Map<String, Long> createStorage(@RequestBody Storage storage) {
        Storage savedStorage = storageService.createStorage(storage);
        Map<String, Long> responseBody = new HashMap<>();
        responseBody.put("id", savedStorage.getId());
        return responseBody;
    }

    @GetMapping("/{id}")
    public ResponseEntity<Storage> getStorage(@PathVariable Long id) {
        return storageService.getStorageById(id)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new StorageNotFoundException("The storage with the specified id does not exist"));
    }

    @GetMapping
    public ResponseEntity<List<Storage>> getAllStorages() {
        List<Storage> storages = storageService.getAllStorages();
        return ResponseEntity.ok(storages);
    }

    @DeleteMapping
    public ResponseEntity<?> deleteStorages(@RequestParam("ids") String idsCSV) {
        if (idsCSV.length() >= 200) {
            throw new InvalidParameterException("CSV length must be less than 200 characters");
        }
        try {
            List<Long> deletedIds = storageService.deleteStorages(idsCSV);
            return ResponseEntity.ok().body(Collections.singletonMap("ids", deletedIds));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

}