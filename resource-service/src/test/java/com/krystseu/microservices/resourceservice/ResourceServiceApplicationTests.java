package com.krystseu.microservices.resourceservice;

import com.krystseu.microservices.songservice.service.SongService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest
class ResourceServiceApplicationTests {

	@MockBean
	private SongService songService;

	@Test
	void contextLoads() {
	}

}