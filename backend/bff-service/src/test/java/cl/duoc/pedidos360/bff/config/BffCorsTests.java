package cl.duoc.pedidos360.bff.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "SPRING_PROFILES_ACTIVE=local")
@AutoConfigureMockMvc
@Import(TestSecurityConfig.class)
class BffCorsTests {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void allowsAngularDevServerPreflight() throws Exception {
		mockMvc.perform(options("/api/pedidos")
						.header("Origin", "http://localhost:4200")
						.header("Access-Control-Request-Method", "POST")
						.header("Access-Control-Request-Headers", "authorization,content-type"))
				.andExpect(status().isOk())
				.andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:4200"));
	}

}
