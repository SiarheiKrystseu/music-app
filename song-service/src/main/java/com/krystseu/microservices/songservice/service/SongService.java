package com.krystseu.microservices.songservice.service;

import com.krystseu.microservices.songservice.dto.SongDTO;

import java.util.List;
import java.util.Optional;

public interface SongService {
    List<SongDTO> getAllSongs();
    Optional<SongDTO> getSongById(Integer id);
    SongDTO createSong(SongDTO songDTO);
    SongDTO updateSong(Long id, SongDTO updatedSongDTO);
    List<Integer> deleteSongs(List<Integer> ids);
}
