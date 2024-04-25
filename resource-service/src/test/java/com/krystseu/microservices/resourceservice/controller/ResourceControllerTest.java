package com.krystseu.microservices.resourceservice.controller;

import com.krystseu.microservices.resourceservice.dto.ResourceResponse;
import com.krystseu.microservices.resourceservice.repository.ResourceRepository;
import com.krystseu.microservices.resourceservice.service.ResourceService;
import org.apache.commons.io.IOUtils;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ResourceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ResourceService resourceService;

    @MockBean
    private ResourceRepository resourceRepository;

    @Test
    void testGetResource() throws Exception {
        // Given
        ResourceResponse resourceResponse = new ResourceResponse();
        resourceResponse.setId(1L);
        resourceResponse.setData(new byte[]{1, 2, 3});
        when(resourceService.getResourceById(1)).thenReturn(Optional.of(resourceResponse));

        // When & Then
        mockMvc.perform(MockMvcRequestBuilders.get("/api/resources/1"))
                .andExpect(MockMvcResultMatchers.status().isOk());
    }

    @Test
    void testDeleteResources() throws Exception {
        // Given
        List<Integer> deletedIds = Arrays.asList(1, 2, 3);
        when(resourceService.deleteResources("1,2,3")).thenReturn(deletedIds);

        // When & Then
        mockMvc.perform(MockMvcRequestBuilders.delete("/api/resources?ids=1,2,3"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(jsonPath("$.ids", Matchers.containsInAnyOrder(1, 2, 3)));
    }

    @Test
    void testUploadAudioFile() throws Exception {
        // Load test file from resources
        ClassPathResource resource = new ClassPathResource("Test_Audio.mp3");
        byte[] audioData = IOUtils.toByteArray(resource.getInputStream());

        // Given
        ResourceResponse resourceResponse = new ResourceResponse();
        resourceResponse.setId(1L);
        when(resourceService.uploadAudio(any())).thenReturn(Optional.of(resourceResponse));

        // When & Then
        mockMvc.perform(MockMvcRequestBuilders.post("/api/resources/upload")
                        .content(audioData)
                        .contentType("audio/mpeg"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(1)));
    }
}