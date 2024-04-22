package com.krystseu.microservices.songservice.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.krystseu.microservices.songservice.dto.SongRequest;
import com.krystseu.microservices.songservice.dto.SongResponse;
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
        songs.add(new Song(1L, "Song 1", "Artist 1", "Album 1", "4:30", 123L, "2024"));
        songs.add(new Song(2L, "Song 2", "Artist 2", "Album 2", "4:30", 234L, "2024"));
        when(songRepository.findAll()).thenReturn(songs);

        // Test
        List<SongResponse> songResponses = songService.getAllSongs();

        // Assertions
        assertEquals(songs.size(), songResponses.size());
    }

    @Test
    void testGetSongById() {
        // Mock data
        Long id = 1L;
        Optional<Song> songOptional = Optional.of(new Song(id, "Song 1", "Artist 1", "Album 1", "4:30", 234L, "2024"));
        when(songRepository.findById(id)).thenReturn(songOptional);

        // Test
        Optional<SongResponse> songResponseOptional = songService.getSongById(id);

        // Assertions
        assertTrue(songResponseOptional.isPresent());
        assertEquals(id, songResponseOptional.get().getId());
    }

    @Test
    void testCreateSong() {
        // Mock data
        SongRequest songRequest = new SongRequest( "New Song", "New Artist", "New Album", "4:30", 123L, "2022");
        Song song = new Song(null, "New Song", "New Artist", "New Album", "4:30", 123L, "2022");
        when(songRepository.save(any(Song.class))).thenReturn(song);

        // Test
        SongResponse createdSongResponse = songService.createSong(songRequest);

        // Assertions
        assertNotNull(createdSongResponse);
        assertEquals(songRequest.getName(), createdSongResponse.getName());
        assertEquals(songRequest.getArtist(), createdSongResponse.getArtist());
        assertEquals(songRequest.getAlbum(), createdSongResponse.getAlbum());
        assertEquals(songRequest.getLength(), createdSongResponse.getLength());
        assertEquals(songRequest.getResourceId(), createdSongResponse.getResourceId());
        assertEquals(songRequest.getYear(), createdSongResponse.getYear());
    }

    @Test
    void testUpdateSong() {
        // Mock data
        Long id = 1L;
        SongRequest updatedSongRequest = new SongRequest("Updated Song", "Updated Artist", "Updated Album", "5:00", 456L, "2023");
        Song existingSong = new Song(id, "Existing Song", "Existing Artist", "Existing Album", "4:00", 123L, "2022");
        when(songRepository.findById(id)).thenReturn(Optional.of(existingSong));
        when(songRepository.save(any(Song.class))).thenAnswer(i -> i.getArguments()[0]);

        // Test
        SongResponse updatedSong = songService.updateSong(id, updatedSongRequest);

        // Assertions
        assertNotNull(updatedSong);
        assertEquals(updatedSongRequest.getName(), updatedSong.getName());
        assertEquals(updatedSongRequest.getArtist(), updatedSong.getArtist());
        assertEquals(updatedSongRequest.getAlbum(), updatedSong.getAlbum());
        assertEquals(updatedSongRequest.getLength(), updatedSong.getLength());
        assertEquals(updatedSongRequest.getResourceId(), updatedSong.getResourceId());
        assertEquals(updatedSongRequest.getYear(), updatedSong.getYear());
    }

    @Test
    void testDeleteSongs() {
        // Mock data
        List<Long> ids = List.of(1L, 2L, 3L);
        List<Long> idList = List.of(1L, 2L, 3L);
        doNothing().when(songRepository).deleteAllById(idList);
        when(songRepository.existsById(1L)).thenReturn(true);
        when(songRepository.existsById(2L)).thenReturn(true);
        when(songRepository.existsById(3L)).thenReturn(true);

        // Test
        List<Long> deletedIds = songService.deleteSongs(ids);

        // Assertions
        assertNotNull(deletedIds);
        assertEquals(ids.size(), deletedIds.size());
        assertTrue(deletedIds.containsAll(ids));
    }

    @Test
    void testDeleteSongs_SongNotFoundException() {
        // Mock data
        List<Long> ids = List.of(1L, 2L, 3L);
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