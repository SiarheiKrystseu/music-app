package com.krystseu.microservices.resourceservice.contract;

import io.restassured.module.mockmvc.response.MockMvcResponse;
import io.restassured.module.mockmvc.specification.MockMvcRequestSpecification;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.nio.file.Files;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;
import static io.restassured.module.mockmvc.RestAssuredMockMvc.given;

@SpringBootTest
class ContractVerifierTest extends ContractHttpVerifierBase {

    @Test
    void validate_httpContract() throws Exception {
        // given:
        byte[] fileContent = Files.readAllBytes(Paths.get("src/test/resources/Test_Audio.mp3"));
        MockMvcRequestSpecification request = given().header("Content-Type", "audio/mpeg").body(fileContent);

        // when:
        MockMvcResponse response = given().spec(request).post("/api/resources/upload");

        // then:
        assertThat(response.statusCode()).isEqualTo(201);
    }
}