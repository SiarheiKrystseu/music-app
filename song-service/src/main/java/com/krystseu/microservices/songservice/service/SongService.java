package com.krystseu.microservices.songservice.service;

import com.krystseu.microservices.songservice.dto.SongRequest;
import com.krystseu.microservices.songservice.dto.SongResponse;

import java.util.List;
import java.util.Optional;

public interface SongService {
    List<SongResponse> getAllSongs();
    Optional<SongResponse> getSongById(Long id);
    SongResponse createSong(SongRequest songRequest);
    SongResponse updateSong(Long id, SongRequest songRequest);
    List<Long> deleteSongs(List<Long> ids);
}
