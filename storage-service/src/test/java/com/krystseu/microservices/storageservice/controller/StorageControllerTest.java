package com.krystseu.microservices.storageservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.krystseu.microservices.storageservice.controller.StorageController;
import com.krystseu.microservices.storageservice.model.Storage;
import com.krystseu.microservices.storageservice.model.StorageType;
import com.krystseu.microservices.storageservice.service.StorageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@WebMvcTest(StorageController.class)
class StorageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StorageService storageService;

    @Test
    void testCreateStorage() throws Exception {
        Storage storage = new Storage();
        storage.setId(1L);
        storage.setStorageType(StorageType.STAGING);
        storage.setBucket("bucket");
        storage.setPath("/path");

        when(storageService.createStorage(any(Storage.class))).thenReturn(storage);

        mockMvc.perform(post("/api/storages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(storage)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)));
    }

    @Test
    void testGetAllStorages() throws Exception {
        Storage storage1 = new Storage();
        storage1.setId(1L);
        storage1.setStorageType(StorageType.STAGING);
        storage1.setBucket("bucket1");
        storage1.setPath("/path1");

        Storage storage2 = new Storage();
        storage2.setId(2L);
        storage2.setStorageType(StorageType.PERMANENT);
        storage2.setBucket("bucket2");
        storage2.setPath("/path2");

        when(storageService.getAllStorages()).thenReturn(Arrays.asList(storage1, storage2));

        mockMvc.perform(get("/api/storages")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id", is(1)))
                .andExpect(jsonPath("$[1].id", is(2)));
    }

    @Test
    void testGetStorage() throws Exception {
        Storage storage = new Storage();
        storage.setId(1L);
        storage.setStorageType(StorageType.STAGING);
        storage.setBucket("bucket");
        storage.setPath("/path");

        when(storageService.getStorageById(1L)).thenReturn(Optional.of(storage));

        mockMvc.perform(get("/api/storages/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.storageType", is("STAGING")))
                .andExpect(jsonPath("$.bucket", is("bucket")))
                .andExpect(jsonPath("$.path", is("/path")));
    }

    @Test
    void testDeleteStorages() throws Exception {
        String idsCSV = "1,2,3";
        List<Long> ids = Arrays.asList(1L, 2L, 3L);

        when(storageService.deleteStorages(idsCSV)).thenReturn(ids);

        mockMvc.perform(delete("/api/storages")
                        .param("ids", idsCSV)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ids[0]", is(1)))
                .andExpect(jsonPath("$.ids[1]", is(2)))
                .andExpect(jsonPath("$.ids[2]", is(3)));
    }
}