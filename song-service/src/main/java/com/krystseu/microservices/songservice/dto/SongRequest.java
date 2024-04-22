package com.krystseu.microservices.songservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SongRequest {
    private String name;
    private String artist;
    private String album;
    private String length;
    private Long resourceId;
    private String year;
}
