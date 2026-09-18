package cl.duoc.pedidos360.productos.controller;

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

import cl.duoc.pedidos360.productos.dto.ProductoRequest;
import cl.duoc.pedidos360.productos.dto.ProductoResponse;
import cl.duoc.pedidos360.productos.exception.ResourceNotFoundException;
import cl.duoc.pedidos360.productos.service.ProductoService;

@WebMvcTest(ProductoController.class)
class ProductoControllerTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockBean
	private ProductoService productoService;

	private ProductoResponse sampleResponse() {
		return new ProductoResponse(
				1L,
				"Monitor 27",
				"Monitor 4K IPS",
				new BigDecimal("299.99"),
				15,
				true,
				LocalDateTime.now(),
				null
		);
	}

	@Test
	void shouldCrearProducto() throws Exception {
		ProductoRequest request = new ProductoRequest("Monitor 27", "Monitor 4K IPS", new BigDecimal("299.99"), 15);
		when(productoService.crear(any(ProductoRequest.class))).thenReturn(sampleResponse());

		mockMvc.perform(post("/api/productos")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").value(1))
				.andExpect(jsonPath("$.nombre").value("Monitor 27"));
	}

	@Test
	void shouldReturn400WhenNombreVacio() throws Exception {
		ProductoRequest invalid = new ProductoRequest("", "Desc", new BigDecimal("10.00"), 5);

		mockMvc.perform(post("/api/productos")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(invalid)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value(400));
	}

	@Test
	void shouldReturn400WhenPrecioNegativo() throws Exception {
		ProductoRequest invalid = new ProductoRequest("Monitor", "Desc", new BigDecimal("-5.00"), 5);

		mockMvc.perform(post("/api/productos")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(invalid)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value(400));
	}

	@Test
	void shouldReturn400WhenStockNegativo() throws Exception {
		ProductoRequest invalid = new ProductoRequest("Monitor", "Desc", new BigDecimal("10.00"), -1);

		mockMvc.perform(post("/api/productos")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(invalid)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value(400));
	}

	@Test
	void shouldListarProductos() throws Exception {
		when(productoService.listar()).thenReturn(List.of(sampleResponse()));

		mockMvc.perform(get("/api/productos"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].nombre").value("Monitor 27"));
	}

	@Test
	void shouldBuscarPorId() throws Exception {
		when(productoService.buscarPorId(1L)).thenReturn(sampleResponse());

		mockMvc.perform(get("/api/productos/1"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(1));
	}

	@Test
	void shouldReturn404WhenBuscarInexistente() throws Exception {
		when(productoService.buscarPorId(99L)).thenThrow(new ResourceNotFoundException("Producto no encontrado"));

		mockMvc.perform(get("/api/productos/99"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.status").value(404));
	}

	@Test
	void shouldActualizarProducto() throws Exception {
		ProductoRequest request = new ProductoRequest("Monitor 27 Pro", "Desc", new BigDecimal("350.00"), 20);
		when(productoService.actualizar(eq(1L), any(ProductoRequest.class))).thenReturn(sampleResponse());

		mockMvc.perform(put("/api/productos/1")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(1));
	}

	@Test
	void shouldEliminarProducto() throws Exception {
		doNothing().when(productoService).eliminar(1L);

		mockMvc.perform(delete("/api/productos/1"))
				.andExpect(status().isNoContent());
	}

	@Test
	void shouldReturn404WhenEliminarInexistente() throws Exception {
		doThrow(new ResourceNotFoundException("No encontrado")).when(productoService).eliminar(99L);

		mockMvc.perform(delete("/api/productos/99"))
				.andExpect(status().isNotFound());
	}

}
