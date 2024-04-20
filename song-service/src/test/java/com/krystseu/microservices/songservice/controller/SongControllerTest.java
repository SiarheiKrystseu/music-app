package com.krystseu.microservices.songservice.controller;

import com.krystseu.microservices.songservice.dto.SongDTO;
import com.krystseu.microservices.songservice.service.SongService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.util.Arrays;
import java.util.Optional;

@SpringBootTest
@AutoConfigureMockMvc
class SongControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SongService songService;

    private SongDTO songDTO;

    @BeforeEach
    public void setUp() {
        songDTO = new SongDTO(1L, "Test Song", "Test Artist", "Test Album", "3:30", "123", "2021");
    }

    @Test
    void testGetAllSongs() throws Exception {
        Mockito.when(songService.getAllSongs()).thenReturn(Arrays.asList(songDTO));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/songs"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].name").value("Test Song"));
    }

    @Test
    void testGetSongById() throws Exception {
        Mockito.when(songService.getSongById(Mockito.anyInt())).thenReturn(Optional.of(songDTO));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/songs/1"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.name").value("Test Song"));
    }

    @Test
    void testCreateSong() throws Exception {
        Mockito.when(songService.createSong(Mockito.any(SongDTO.class))).thenReturn(songDTO);

        mockMvc.perform(MockMvcRequestBuilders.post("/api/songs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"name\": \"Test Song\", \"artist\": \"Test Artist\", \"album\": \"Test Album\", \"length\": \"3:30\", \"resourceId\": \"123\", \"release\": \"2021\" }"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.id").value("1"));
    }

}