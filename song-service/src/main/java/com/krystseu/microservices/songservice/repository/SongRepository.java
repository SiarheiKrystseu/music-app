package com.krystseu.microservices.songservice.repository;

import com.krystseu.microservices.songservice.model.Song;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SongRepository extends JpaRepository<Song, Long> {
}
