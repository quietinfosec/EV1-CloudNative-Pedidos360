package cl.duoc.pedidos360.bff;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import cl.duoc.pedidos360.bff.config.TestSecurityConfig;

@SpringBootTest(properties = "SPRING_PROFILES_ACTIVE=local")
@AutoConfigureMockMvc
@Import(TestSecurityConfig.class)
class BffServiceApplicationTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	Environment environment;

	@Test
	void contextLoads() {
		assertThat(environment.getProperty("server.port", Integer.class)).isEqualTo(8080);
	}

	@Test
	void healthEndpointReturnsUp() throws Exception {
		mockMvc.perform(MockMvcRequestBuilders.get("/api/health"))
				.andExpect(MockMvcResultMatchers.status().isOk())
				.andExpect(MockMvcResultMatchers.jsonPath("$.service").value("bff-service"))
				.andExpect(MockMvcResultMatchers.jsonPath("$.status").value("UP"));
	}

	@Test
	void actuatorHealthEndpointReturnsUp() throws Exception {
		mockMvc.perform(MockMvcRequestBuilders.get("/actuator/health"))
				.andExpect(MockMvcResultMatchers.status().isOk())
				.andExpect(MockMvcResultMatchers.jsonPath("$.status").value("UP"));
	}

}
