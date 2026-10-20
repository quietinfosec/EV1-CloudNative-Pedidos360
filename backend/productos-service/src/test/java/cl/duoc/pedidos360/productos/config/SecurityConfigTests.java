package cl.duoc.pedidos360.productos.config;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Verifica el modo seguro (Resource Server): sin token -> 401, con token -> 200.
 */
@SpringBootTest(properties = "pedidos360.security.require-auth=true")
@AutoConfigureMockMvc
@Import(TestSecurityConfig.class)
class SecurityConfigTests {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void sinTokenRespondeNoAutorizado() throws Exception {
		mockMvc.perform(get("/api/productos"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void conJwtValidoAccede() throws Exception {
		mockMvc.perform(get("/api/productos").with(jwt()))
				.andExpect(status().isOk());
	}

	@Test
	void healthEsPublicoSinToken() throws Exception {
		mockMvc.perform(get("/api/health")).andExpect(status().isOk());
		mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
	}

}
