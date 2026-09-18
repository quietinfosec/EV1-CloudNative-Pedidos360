package cl.duoc.pedidos360.pedidos.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import cl.duoc.pedidos360.pedidos.entity.EstadoPedido;
import cl.duoc.pedidos360.pedidos.entity.Pedido;

@DataJpaTest
class PedidoRepositoryTests {

	@Autowired
	private PedidoRepository pedidoRepository;

	private Pedido pedido;

	@BeforeEach
	void setUp() {
		pedidoRepository.deleteAll();
		pedido = new Pedido(null, "usuario@duoc.cl", LocalDateTime.now(), EstadoPedido.PENDIENTE, new BigDecimal("120.50"));
		pedido = pedidoRepository.save(pedido);
	}

	@Test
	void shouldSaveAndFindById() {
		Optional<Pedido> found = pedidoRepository.findById(pedido.getId());
		assertThat(found).isPresent();
		assertThat(found.get().getUsuario()).isEqualTo("usuario@duoc.cl");
		assertThat(found.get().getEstado()).isEqualTo(EstadoPedido.PENDIENTE);
		assertThat(found.get().getTotal()).isEqualByComparingTo("120.50");
	}

	@Test
	void shouldFindByUsuario() {
		List<Pedido> pedidos = pedidoRepository.findByUsuario("usuario@duoc.cl");
		assertThat(pedidos).hasSize(1);
		assertThat(pedidos.get(0).getUsuario()).isEqualTo("usuario@duoc.cl");
	}

	@Test
	void shouldFindByEstado() {
		List<Pedido> pedidos = pedidoRepository.findByEstado(EstadoPedido.PENDIENTE);
		assertThat(pedidos).hasSize(1);
	}

	@Test
	void shouldDeletePedido() {
		pedidoRepository.deleteById(pedido.getId());
		assertThat(pedidoRepository.findById(pedido.getId())).isEmpty();
	}

}
