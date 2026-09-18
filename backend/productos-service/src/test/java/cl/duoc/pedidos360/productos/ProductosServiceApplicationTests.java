package cl.duoc.pedidos360.productos;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class ProductosServiceApplicationTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	Environment environment;

	@Test
	void contextLoads() {
		assertThat(environment.getProperty("server.port", Integer.class)).isEqualTo(8082);
	}

	@Test
	void healthEndpointReturnsUp() throws Exception {
		mockMvc.perform(MockMvcRequestBuilders.get("/api/health"))
				.andExpect(MockMvcResultMatchers.status().isOk())
				.andExpect(MockMvcResultMatchers.jsonPath("$.service").value("productos-service"))
				.andExpect(MockMvcResultMatchers.jsonPath("$.status").value("UP"));
	}

	@Test
	void actuatorHealthEndpointReturnsUp() throws Exception {
		mockMvc.perform(MockMvcRequestBuilders.get("/actuator/health"))
				.andExpect(MockMvcResultMatchers.status().isOk())
				.andExpect(MockMvcResultMatchers.jsonPath("$.status").value("UP"));
	}

}
