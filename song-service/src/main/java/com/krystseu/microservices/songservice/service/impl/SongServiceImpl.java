package com.krystseu.microservices.songservice.service.impl;

import com.krystseu.microservices.songservice.dto.SongDTO;
import com.krystseu.microservices.songservice.exception.SongNotFoundException;
import com.krystseu.microservices.songservice.model.Song;
import com.krystseu.microservices.songservice.repository.SongRepository;
import com.krystseu.microservices.songservice.service.SongService;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
    public List<SongDTO> getAllSongs() {
        List<Song> songs = songRepository.findAll();
        return songs.stream()
                .map(this::convertToDTO).toList();
    }

    @Override
    public Optional<SongDTO> getSongById(Integer id) {
        Optional<Song> optionalSong = songRepository.findById(Long.valueOf(id));
        return optionalSong.map(this::convertToDTO);
    }

    @Override
    public SongDTO createSong(SongDTO songDTO) {
        Song song = convertToEntity(songDTO);
        Song createdSong = songRepository.save(song);
        return convertToDTO(createdSong);
    }

    @Override
    public SongDTO updateSong(Long id, SongDTO updatedSongDTO) {
        Optional<Song> optionalExistingSong = songRepository.findById(id);
        if (optionalExistingSong.isPresent()) {
            Song existingSong = optionalExistingSong.get();
            BeanUtils.copyProperties(updatedSongDTO, existingSong);
            Song updatedSong = songRepository.save(existingSong);
            return convertToDTO(updatedSong);
        } else {
            throw new SongNotFoundException("Song not found with id: " + id);
        }
    }

    @Override
    public List<Integer> deleteSongs(List<Integer> ids) {
        List<Long> idList = ids.stream().map(Long::valueOf).toList();
        for(Long id : idList) {
            if(!songRepository.existsById(id)) {
                throw new SongNotFoundException("Song with id " + id + " not found");
            }
        }
        songRepository.deleteAllById(idList);
        return ids;
    }

    private SongDTO convertToDTO(Song song) {
        SongDTO songDTO = new SongDTO();
        BeanUtils.copyProperties(song, songDTO);
        return songDTO;
    }

    private Song convertToEntity(SongDTO songDTO) {
        Song song = new Song();
        BeanUtils.copyProperties(songDTO, song);
        return song;
    }
}
