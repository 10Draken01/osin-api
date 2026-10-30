package com.lacomer.osinapi;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

// Perfil "test" con H2 en memoria, no necesita Oracle
@SpringBootTest
@ActiveProfiles("test")
class OsinapiApplicationTests {

	@Test
	void contextLoads() {
	}

}
