package com.krystseu.microservices.songservice.service;

import com.krystseu.microservices.songservice.dto.SongRequest;
import com.krystseu.microservices.songservice.dto.SongResponse;

import java.util.List;
import java.util.Optional;

public interface SongService {
    List<SongResponse> getAllSongs();
    Optional<SongResponse> getSongById(Integer id);
    SongResponse createSong(SongRequest songRequest);
    SongResponse updateSong(Integer id, SongRequest songRequest);
    List<Integer> deleteSongs(String idsCSV);
}
