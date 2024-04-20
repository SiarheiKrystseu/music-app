package com.krystseu.microservices.songservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SongDTO {
    private Long id;
    private String name;
    private String artist;
    private String album;
    private String length;
    private String resourceId;
    private String release;
}
