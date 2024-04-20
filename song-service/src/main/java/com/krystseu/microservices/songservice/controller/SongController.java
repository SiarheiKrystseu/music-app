package com.krystseu.microservices.songservice.controller;

import com.krystseu.microservices.songservice.dto.SongDTO;
import com.krystseu.microservices.songservice.exception.SongNotFoundException;
import com.krystseu.microservices.songservice.service.SongService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/songs")
public class SongController {

    private final SongService songService;

    @Autowired
    public SongController(SongService songService) {
        this.songService = songService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getSongById(@PathVariable("id") Integer id) {
        try {
            Optional<SongDTO> songDTO = songService.getSongById(id);
            if (songDTO.isPresent()) {
                return ResponseEntity.ok(songDTO);
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("The song metadata with the specified id does not exist");
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An internal server error has occurred");
        }
    }

    @GetMapping
    public ResponseEntity<List<SongDTO>> getAllSongs() {
        List<SongDTO> songs = songService.getAllSongs();
        return new ResponseEntity<>(songs, HttpStatus.OK);
    }

    @PostMapping
    public ResponseEntity<?> createSong(@Valid @RequestBody(required = false) SongDTO songDTO, BindingResult bindingResult) {
        if (songDTO == null) {
            return ResponseEntity.badRequest().body("Request body cannot be empty");
        }

        if (bindingResult.hasErrors()) {
            return ResponseEntity.badRequest().body(bindingResult.getAllErrors());
        }

        try {
            SongDTO createdSongDTO = songService.createSong(songDTO);
            return ResponseEntity.ok(Collections.singletonMap("id", createdSongDTO.getId()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An internal server error has occurred");
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<SongDTO> updateSong(@PathVariable(name = "id") Long id, @RequestBody SongDTO updatedSongDTO) {
        SongDTO updatedSongResponseDTO = songService.updateSong(id, updatedSongDTO);
        return new ResponseEntity<>(updatedSongResponseDTO, HttpStatus.OK);
    }

    @DeleteMapping
    public ResponseEntity<?> deleteSongs(@RequestParam("id") List<Integer> ids) {
        try {
            List<Integer> deletedIds = songService.deleteSongs(ids);
            return ResponseEntity.ok().body(Collections.singletonMap("ids", deletedIds));
        } catch (SongNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}

