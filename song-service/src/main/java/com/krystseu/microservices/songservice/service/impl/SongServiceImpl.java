package com.krystseu.microservices.songservice.service.impl;

import com.krystseu.microservices.songservice.dto.SongRequest;
import com.krystseu.microservices.songservice.dto.SongResponse;
import com.krystseu.microservices.songservice.exception.SongNotFoundException;
import com.krystseu.microservices.songservice.model.Song;
import com.krystseu.microservices.songservice.repository.SongRepository;
import com.krystseu.microservices.songservice.service.SongService;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
@Slf4j
public class SongServiceImpl implements SongService {

    private final SongRepository songRepository;

    @Autowired
    public SongServiceImpl(SongRepository songRepository) {
        this.songRepository = songRepository;
    }

    @Override
    public List<SongResponse> getAllSongs() {
        List<Song> songs = songRepository.findAll();
        return songs.stream()
                .map(this::convertToSongResponse).toList();
    }

    @Override
    public Optional<SongResponse> getSongById(Integer id) {
        Optional<Song> optionalSong = songRepository.findById(Long.valueOf(id));
        return optionalSong.map(this::convertToSongResponse);
    }

    @Override
    public SongResponse createSong(SongRequest songRequest) {
        Song song = convertToEntity(songRequest);
        Song createdSong = songRepository.save(song);
        return convertToSongResponse(createdSong);
    }

    @Override
    public SongResponse updateSong(Integer id, SongRequest updatedSongRequest) {
        Optional<Song> optionalExistingSong = songRepository.findById(Long.valueOf(id));
        if (optionalExistingSong.isPresent()) {
            Song existingSong = optionalExistingSong.get();

            // Update properties using builder pattern
            Song updatedSong = Song.builder()
                    .id(existingSong.getId())
                    .name(updatedSongRequest.getName())
                    .artist(updatedSongRequest.getArtist())
                    .album(updatedSongRequest.getAlbum())
                    .length(updatedSongRequest.getLength())
                    .resourceId(updatedSongRequest.getResourceId())
                    .year(updatedSongRequest.getYear())
                    .build();

            updatedSong = songRepository.save(updatedSong);
            return convertToSongResponse(updatedSong);
        } else {
            throw new SongNotFoundException("Song not found with id: " + id);
        }
    }

    @Override
    public List<Integer> deleteSongs(String idsCSV) {
        List<Integer> integerIds;
        try {
            // Split the CSV string into individual IDs
            integerIds = Arrays.stream(idsCSV.split(","))
                    .map(Integer::parseInt)
                    .toList();
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid CSV format: all values must be integers");
        }


        List<Long> longIds = integerIds.stream()
                .map(Integer::longValue)
                .toList();

        for(Long id : longIds) {
            if(!songRepository.existsById(id)) {
                throw new SongNotFoundException("Song with id " + id + " not found");
            }
        }
        songRepository.deleteAllById(longIds);
        return integerIds;
    }

    private SongResponse convertToSongResponse(Song song) {
        return SongResponse.builder()
                .id(song.getId())
                .name(song.getName())
                .artist(song.getArtist())
                .album(song.getAlbum())
                .length(song.getLength())
                .resourceId(song.getResourceId())
                .year(song.getYear())
                .build();
    }

    private Song convertToEntity(SongRequest songRequest) {
        return Song.builder()
                .name(songRequest.getName())
                .artist(songRequest.getArtist())
                .album(songRequest.getAlbum())
                .length(songRequest.getLength())
                .resourceId(songRequest.getResourceId())
                .year(songRequest.getYear())
                .build();
    }
}
