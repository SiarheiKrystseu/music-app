package com.krystseu.microservices.resourceservice.service;

import com.krystseu.microservices.resourceservice.dto.ResourceResponse;
import com.krystseu.microservices.resourceservice.model.Resource;
import com.krystseu.microservices.songservice.dto.SongResponse;
import org.apache.tika.exception.TikaException;
import org.springframework.web.multipart.MultipartFile;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

public interface ResourceService {
    Optional<ResourceResponse> uploadFile(MultipartFile file) throws IOException, TikaException, SAXException;
    Optional<ResourceResponse> getResourceById(Integer id);
    Optional<SongResponse> getSongByResourceId(Integer id);
    List<Integer> deleteResources(List<Integer> ids);
}
