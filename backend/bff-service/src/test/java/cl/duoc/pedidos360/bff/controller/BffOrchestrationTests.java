package cl.duoc.pedidos360.bff.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import cl.duoc.pedidos360.bff.client.PedidosClient;
import cl.duoc.pedidos360.bff.client.ProductosClient;
import cl.duoc.pedidos360.bff.config.TestSecurityConfig;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
@Import(TestSecurityConfig.class)
class BffOrchestrationTests {

	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private ProductosClient productosClient;

	@MockBean
	private PedidosClient pedidosClient;

	@Test
	void shouldAllowPublicGetProductos() throws Exception {
		when(productosClient.getAll(anyBoolean()))
				.thenReturn(ResponseEntity.ok("[{\"id\":1,\"nombre\":\"Teclado\"}]"));

		mockMvc.perform(get("/api/productos"))
				.andExpect(status().isOk())
				.andExpect(content().json("[{\"id\":1,\"nombre\":\"Teclado\"}]"));
	}

	@Test
	void shouldRejectPostProductosWithoutToken() throws Exception {
		mockMvc.perform(post("/api/productos")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"nombre\":\"Mouse\",\"precio\":10.0}"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void shouldAllowPostProductosWithValidJwt() throws Exception {
		when(productosClient.create(any()))
				.thenReturn(ResponseEntity.status(HttpStatus.CREATED).body("{\"id\":2,\"nombre\":\"Mouse\"}"));

		mockMvc.perform(post("/api/productos")
						.with(jwt())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"nombre\":\"Mouse\",\"precio\":10.0}"))
				.andExpect(status().isCreated())
				.andExpect(content().json("{\"id\":2,\"nombre\":\"Mouse\"}"));
	}

	@Test
	void shouldRejectGetPedidosWithoutToken() throws Exception {
		mockMvc.perform(get("/api/pedidos"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void shouldAllowGetPedidosWithValidJwt() throws Exception {
		when(pedidosClient.getAll(any()))
				.thenReturn(ResponseEntity.ok("[{\"id\":1,\"usuario\":\"juan@duoc.cl\"}]"));

		mockMvc.perform(get("/api/pedidos")
						.with(jwt()))
				.andExpect(status().isOk())
				.andExpect(content().json("[{\"id\":1,\"usuario\":\"juan@duoc.cl\"}]"));
	}

	@Test
	void shouldAllowDeleteWithValidJwt() throws Exception {
		when(productosClient.delete(eq(1L)))
				.thenReturn(ResponseEntity.noContent().build());

		mockMvc.perform(delete("/api/productos/1")
						.with(jwt()))
				.andExpect(status().isNoContent());
	}

}
