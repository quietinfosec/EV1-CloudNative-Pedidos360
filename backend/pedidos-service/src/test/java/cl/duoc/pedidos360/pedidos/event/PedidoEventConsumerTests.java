package cl.duoc.pedidos360.pedidos.event;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import cl.duoc.pedidos360.pedidos.config.RabbitMQConfig;

class PedidoEventConsumerTests {

	private PedidoEventConsumer consumer;
	private PedidoEvent event;

	@BeforeEach
	void setUp() {
		consumer = new PedidoEventConsumer();
		event = new PedidoEvent(
				RabbitMQConfig.PEDIDO_CREADO,
				1001L,
				"PENDIENTE",
				"usuario-1",
				new BigDecimal("29990"),
				null
		);
	}

	@Test
	void shouldConsumeValidPedidoEvent() {
		assertThatCode(() -> consumer.consumeEvento(event)).doesNotThrowAnyException();
	}

	@Test
	void shouldRejectPedidoEventWithoutEventId() {
		event.setEventId(null);

		assertThatThrownBy(() -> consumer.consumeEvento(event))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void shouldRejectPedidoEventWithBlankEventType() {
		event.setEventType(" ");

		assertThatThrownBy(() -> consumer.consumeEvento(event))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void shouldRejectPedidoEventWithoutPedidoId() {
		event.setPedidoId(null);

		assertThatThrownBy(() -> consumer.consumeEvento(event))
				.isInstanceOf(IllegalArgumentException.class);
	}

}
