package com.krystseu.microservices.resourceservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResourceResponse {
    private Long id;
    private byte[] data;
}
