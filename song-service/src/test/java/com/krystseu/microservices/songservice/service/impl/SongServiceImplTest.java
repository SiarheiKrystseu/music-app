package com.krystseu.microservices.songservice.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.krystseu.microservices.songservice.dto.SongDTO;
import com.krystseu.microservices.songservice.exception.SongNotFoundException;
import com.krystseu.microservices.songservice.model.Song;
import com.krystseu.microservices.songservice.repository.SongRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

class SongServiceImplTest {

    @Mock
    private SongRepository songRepository;

    @InjectMocks
    private SongServiceImpl songService;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    void testGetAllSongs() {
        // Mock data
        List<Song> songs = new ArrayList<>();
        songs.add(new Song(1L, "Song 1", "Artist 1", "Album 1", "4:30", "123", "2024-04-19"));
        songs.add(new Song(2L, "Song 2", "Artist 2", "Album 2", "4:30", "234", "2024-04-19"));
        when(songRepository.findAll()).thenReturn(songs);

        // Test
        List<SongDTO> songDTOs = songService.getAllSongs();

        // Assertions
        assertEquals(songs.size(), songDTOs.size());
    }

    @Test
    void testGetSongById() {
        // Mock data
        Long id = 1L;
        Optional<Song> songOptional = Optional.of(new Song(id, "Song 1", "Artist 1", "Album 1", "4:30", "234", "2024-04-19"));
        when(songRepository.findById(id)).thenReturn(songOptional);

        // Test
        Optional<SongDTO> songDTOOptional = songService.getSongById(id.intValue());

        // Assertions
        assertTrue(songDTOOptional.isPresent());
        assertEquals(id, songDTOOptional.get().getId());
    }

    @Test
    void testCreateSong() {
        // Mock data
        SongDTO songDTO = new SongDTO(null, "New Song", "New Artist", "New Album", "4:30", "123", "2022");
        Song song = new Song(null, "New Song", "New Artist", "New Album", "4:30", "123", "2022");
        when(songRepository.save(any(Song.class))).thenReturn(song);

        // Test
        SongDTO createdSongDTO = songService.createSong(songDTO);

        // Assertions
        assertNotNull(createdSongDTO);
        assertEquals(songDTO.getName(), createdSongDTO.getName());
        assertEquals(songDTO.getArtist(), createdSongDTO.getArtist());
        assertEquals(songDTO.getAlbum(), createdSongDTO.getAlbum());
        assertEquals(songDTO.getLength(), createdSongDTO.getLength());
        assertEquals(songDTO.getResourceId(), createdSongDTO.getResourceId());
        assertEquals(songDTO.getRelease(), createdSongDTO.getRelease());
    }

    @Test
    void testUpdateSong() {
        // Mock data
        Long id = 1L;
        SongDTO updatedSongDTO = new SongDTO(id, "Updated Song", "Updated Artist", "Updated Album", "5:00", "456", "2023");
        Song existingSong = new Song(id, "Existing Song", "Existing Artist", "Existing Album", "4:00", "123", "2022");
        when(songRepository.findById(id)).thenReturn(Optional.of(existingSong));
        when(songRepository.save(any(Song.class))).thenReturn(existingSong);

        // Test
        SongDTO updatedSong = songService.updateSong(id, updatedSongDTO);

        // Assertions
        assertNotNull(updatedSong);
        assertEquals(updatedSongDTO.getName(), updatedSong.getName());
        assertEquals(updatedSongDTO.getArtist(), updatedSong.getArtist());
        assertEquals(updatedSongDTO.getAlbum(), updatedSong.getAlbum());
        assertEquals(updatedSongDTO.getLength(), updatedSong.getLength());
        assertEquals(updatedSongDTO.getResourceId(), updatedSong.getResourceId());
        assertEquals(updatedSongDTO.getRelease(), updatedSong.getRelease());
    }

    @Test
    void testDeleteSongs() {
        // Mock data
        List<Integer> ids = List.of(1, 2, 3);
        List<Long> idList = List.of(1L, 2L, 3L);
        doNothing().when(songRepository).deleteAllById(idList);

        // Test
        List<Integer> deletedIds = songService.deleteSongs(ids);

        // Assertions
        assertNotNull(deletedIds);
        assertEquals(ids.size(), deletedIds.size());
        assertTrue(deletedIds.containsAll(ids));
    }

    @Test
    void testDeleteSongs_SongNotFoundException() {
        // Mock data
        List<Integer> ids = List.of(1, 2, 3);
        List<Long> idList = List.of(1L, 2L, 3L);

        // Mock behavior
        when(songRepository.existsById(1L)).thenReturn(true);
        when(songRepository.existsById(2L)).thenReturn(true);
        when(songRepository.existsById(3L)).thenReturn(false); // One song does not exist

        // Test
        Exception exception = assertThrows(SongNotFoundException.class, () -> {
            songService.deleteSongs(ids);
        });

        // Assertions
        String expectedMessage = "Song with id 3 not found";
        String actualMessage = exception.getMessage();
        assertTrue(actualMessage.contains(expectedMessage));
    }
}