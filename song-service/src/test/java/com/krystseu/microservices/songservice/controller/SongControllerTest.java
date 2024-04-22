package com.krystseu.microservices.songservice.controller;

import com.krystseu.microservices.songservice.service.SongService;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;


@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class SongControllerTest {

    @Container
    static PostgreSQLContainer postgreSQLContainer = new PostgreSQLContainer<>("postgres:16.2")
            .withDatabaseName("test")
            .withUsername("test")
            .withPassword("test")
            .withInitScript("init.sql");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SongService songService;

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry dynamicPropertyRegistry) {
        dynamicPropertyRegistry.add("spring.datasource.url", postgreSQLContainer::getJdbcUrl);
        dynamicPropertyRegistry.add("spring.datasource.username", postgreSQLContainer::getUsername);
        dynamicPropertyRegistry.add("spring.datasource.password", postgreSQLContainer::getPassword);
    }

    @BeforeEach
    public void setUp() {
        postgreSQLContainer.start();
    }

    @Test
    void testGetAllSongs() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/songs"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$", Matchers.hasSize(3))); // Asserts that the response is a JSON array of size 3
    }

    @Test
    void testGetSongById() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/songs/1"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.name", Matchers.is("Song1"))) // Asserts that the 'name' field of the JSON response is 'Song1'
                .andExpect(MockMvcResultMatchers.jsonPath("$.artist", Matchers.is("Artist1"))) // Asserts that the 'artist' field of the JSON response is 'Artist1'
                .andExpect(MockMvcResultMatchers.jsonPath("$.album", Matchers.is("Album1"))); // Asserts that the 'album' field of the JSON response is 'Album1'
    }

    @Test
    void testCreateSong() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/api/songs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"name\": \"Test Song\", \"artist\": \"Test Artist\", \"album\": \"Test Album\", \"length\": \"3:30\", \"resourceId\": 123, \"release\": \"2021\" }"))
                .andExpect(MockMvcResultMatchers.status().isCreated())
                .andExpect(MockMvcResultMatchers.jsonPath("$.id").value("4"));
    }

}