package cl.duoc.pedidos360.pedidos.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

import cl.duoc.pedidos360.pedidos.dto.PedidoRequest;
import cl.duoc.pedidos360.pedidos.dto.PedidoResponse;
import cl.duoc.pedidos360.pedidos.entity.EstadoPedido;
import cl.duoc.pedidos360.pedidos.exception.ResourceNotFoundException;
import cl.duoc.pedidos360.pedidos.service.PedidoService;

@WebMvcTest(PedidoController.class)
class PedidoControllerTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockBean
	private PedidoService pedidoService;

	private PedidoResponse sampleResponse() {
		return new PedidoResponse(
				1L,
				"usuario@duoc.cl",
				LocalDateTime.now(),
				EstadoPedido.PENDIENTE,
				new BigDecimal("150.00"),
				LocalDateTime.now(),
				null
		);
	}

	@Test
	void shouldCrearPedido() throws Exception {
		PedidoRequest request = new PedidoRequest("usuario@duoc.cl", new BigDecimal("150.00"), EstadoPedido.PENDIENTE);
		when(pedidoService.crear(any(PedidoRequest.class))).thenReturn(sampleResponse());

		mockMvc.perform(post("/api/pedidos")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").value(1))
				.andExpect(jsonPath("$.usuario").value("usuario@duoc.cl"))
				.andExpect(jsonPath("$.estado").value("PENDIENTE"));
	}

	@Test
	void shouldReturn400WhenUsuarioVacio() throws Exception {
		PedidoRequest invalidRequest = new PedidoRequest("", new BigDecimal("50.00"), EstadoPedido.PENDIENTE);

		mockMvc.perform(post("/api/pedidos")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(invalidRequest)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.validationErrors").exists());
	}

	@Test
	void shouldReturn400WhenTotalInvalido() throws Exception {
		PedidoRequest invalidRequest = new PedidoRequest("usuario@duoc.cl", new BigDecimal("-10.00"), EstadoPedido.PENDIENTE);

		mockMvc.perform(post("/api/pedidos")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(invalidRequest)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value(400));
	}

	@Test
	void shouldListarPedidos() throws Exception {
		when(pedidoService.listar()).thenReturn(List.of(sampleResponse()));

		mockMvc.perform(get("/api/pedidos"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].usuario").value("usuario@duoc.cl"));
	}

	@Test
	void shouldBuscarPorId() throws Exception {
		when(pedidoService.buscarPorId(1L)).thenReturn(sampleResponse());

		mockMvc.perform(get("/api/pedidos/1"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(1));
	}

	@Test
	void shouldReturn404WhenBuscarInexistente() throws Exception {
		when(pedidoService.buscarPorId(99L)).thenThrow(new ResourceNotFoundException("Pedido no encontrado"));

		mockMvc.perform(get("/api/pedidos/99"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.status").value(404));
	}

	@Test
	void shouldBuscarPorUsuario() throws Exception {
		when(pedidoService.listarPorUsuario("usuario@duoc.cl")).thenReturn(List.of(sampleResponse()));

		mockMvc.perform(get("/api/pedidos/usuario/usuario@duoc.cl"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1));
	}

	@Test
	void shouldActualizarPedido() throws Exception {
		PedidoRequest request = new PedidoRequest("usuario@duoc.cl", new BigDecimal("200.00"), EstadoPedido.CONFIRMADO);
		when(pedidoService.actualizar(eq(1L), any(PedidoRequest.class))).thenReturn(sampleResponse());

		mockMvc.perform(put("/api/pedidos/1")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(1));
	}

	@Test
	void shouldEliminarPedido() throws Exception {
		doNothing().when(pedidoService).eliminar(1L);

		mockMvc.perform(delete("/api/pedidos/1"))
				.andExpect(status().isNoContent());
	}

	@Test
	void shouldReturn404WhenEliminarInexistente() throws Exception {
		doThrow(new ResourceNotFoundException("No encontrado")).when(pedidoService).eliminar(99L);

		mockMvc.perform(delete("/api/pedidos/99"))
				.andExpect(status().isNotFound());
	}

}
