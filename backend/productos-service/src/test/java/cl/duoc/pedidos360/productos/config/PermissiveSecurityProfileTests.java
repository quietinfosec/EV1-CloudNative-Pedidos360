package cl.duoc.pedidos360.productos.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

/**
 * EP3 (Fase 6): verifica el modo local permisivo explicito
 * (pedidos360.security.require-auth=false): sin token la API responde 200,
 * lo que permite desarrollo local sin proveedor OIDC.
 */
@SpringBootTest(properties = "pedidos360.security.require-auth=false")
@AutoConfigureMockMvc
class PermissiveSecurityProfileTests {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void sinTokenAccedeEnModoPermisivo() throws Exception {
		mockMvc.perform(get("/api/productos")).andExpect(status().isOk());
	}

}
