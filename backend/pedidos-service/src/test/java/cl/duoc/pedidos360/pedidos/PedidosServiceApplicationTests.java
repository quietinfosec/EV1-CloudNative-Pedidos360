package cl.duoc.pedidos360.pedidos;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

@SpringBootTest(properties = "SPRING_PROFILES_ACTIVE=local")
@AutoConfigureMockMvc
class PedidosServiceApplicationTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	Environment environment;

	@Test
	void contextLoads() {
		assertThat(environment.getProperty("server.port", Integer.class)).isEqualTo(8081);
	}

	@Test
	void healthEndpointReturnsUp() throws Exception {
		mockMvc.perform(MockMvcRequestBuilders.get("/api/health"))
				.andExpect(MockMvcResultMatchers.status().isOk())
				.andExpect(MockMvcResultMatchers.jsonPath("$.service").value("pedidos-service"))
				.andExpect(MockMvcResultMatchers.jsonPath("$.status").value("UP"));
	}

	@Test
	void actuatorHealthEndpointReturnsUp() throws Exception {
		mockMvc.perform(MockMvcRequestBuilders.get("/actuator/health"))
				.andExpect(MockMvcResultMatchers.status().isOk())
				.andExpect(MockMvcResultMatchers.jsonPath("$.status").value("UP"));
	}

}
