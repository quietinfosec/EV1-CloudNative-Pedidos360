package cl.duoc.pedidos360.pedidos.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import cl.duoc.pedidos360.pedidos.dto.PedidoRequest;
import cl.duoc.pedidos360.pedidos.dto.PedidoResponse;
import cl.duoc.pedidos360.pedidos.entity.EstadoPedido;
import cl.duoc.pedidos360.pedidos.entity.Pedido;
import cl.duoc.pedidos360.pedidos.exception.ResourceNotFoundException;
import cl.duoc.pedidos360.pedidos.repository.PedidoRepository;

@ExtendWith(MockitoExtension.class)
class PedidoServiceTests {

	@Mock
	private PedidoRepository pedidoRepository;

	@InjectMocks
	private PedidoServiceImpl pedidoService;

	private Pedido pedido;
	private PedidoRequest request;

	@BeforeEach
	void setUp() {
		pedido = new Pedido(1L, "ana@duoc.cl", LocalDateTime.now(), EstadoPedido.PENDIENTE, new BigDecimal("85.00"));
		request = new PedidoRequest("ana@duoc.cl", new BigDecimal("85.00"), EstadoPedido.PENDIENTE);
	}

	@Test
	void shouldCrearPedido() {
		when(pedidoRepository.save(any(Pedido.class))).thenReturn(pedido);

		PedidoResponse result = pedidoService.crear(request);

		assertThat(result).isNotNull();
		assertThat(result.getUsuario()).isEqualTo("ana@duoc.cl");
		assertThat(result.getEstado()).isEqualTo(EstadoPedido.PENDIENTE);
		verify(pedidoRepository).save(any(Pedido.class));
	}

	@Test
	void shouldListarPedidos() {
		when(pedidoRepository.findAll()).thenReturn(List.of(pedido));

		List<PedidoResponse> result = pedidoService.listar();

		assertThat(result).hasSize(1);
		assertThat(result.get(0).getUsuario()).isEqualTo("ana@duoc.cl");
	}

	@Test
	void shouldListarPorUsuario() {
		when(pedidoRepository.findByUsuario("ana@duoc.cl")).thenReturn(List.of(pedido));

		List<PedidoResponse> result = pedidoService.listarPorUsuario("ana@duoc.cl");

		assertThat(result).hasSize(1);
	}

	@Test
	void shouldBuscarPorId() {
		when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));

		PedidoResponse result = pedidoService.buscarPorId(1L);

		assertThat(result).isNotNull();
		assertThat(result.getId()).isEqualTo(1L);
	}

	@Test
	void shouldThrowNotFoundWhenBuscarIdInexistente() {
		when(pedidoRepository.findById(99L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> pedidoService.buscarPorId(99L))
				.isInstanceOf(ResourceNotFoundException.class);
	}

	@Test
	void shouldActualizarPedido() {
		when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));
		when(pedidoRepository.save(any(Pedido.class))).thenReturn(pedido);

		PedidoRequest updateRequest = new PedidoRequest("ana@duoc.cl", new BigDecimal("99.00"), EstadoPedido.CONFIRMADO);
		PedidoResponse result = pedidoService.actualizar(1L, updateRequest);

		assertThat(result).isNotNull();
		verify(pedidoRepository).save(pedido);
	}

	@Test
	void shouldCancelarPedido() {
		when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));
		when(pedidoRepository.save(any(Pedido.class))).thenReturn(pedido);

		pedidoService.cancelar(1L);

		assertThat(pedido.getEstado()).isEqualTo(EstadoPedido.CANCELADO);
		verify(pedidoRepository).save(pedido);
	}

	@Test
	void shouldEliminarPedido() {
		when(pedidoRepository.existsById(1L)).thenReturn(true);

		pedidoService.eliminar(1L);

		verify(pedidoRepository).deleteById(1L);
	}

	@Test
	void shouldThrowNotFoundWhenEliminarInexistente() {
		when(pedidoRepository.existsById(99L)).thenReturn(false);

		assertThatThrownBy(() -> pedidoService.eliminar(99L))
				.isInstanceOf(ResourceNotFoundException.class);
	}

}
