package cl.duoc.pedidos360.productos.event;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import cl.duoc.pedidos360.productos.config.RabbitMQConfig;

class ProductoEventConsumerTests {

	private ProductoEventConsumer consumer;
	private ProductoEvent event;

	@BeforeEach
	void setUp() {
		consumer = new ProductoEventConsumer();
		event = new ProductoEvent(
				RabbitMQConfig.PRODUCTO_CREADO,
				2001L,
				"Teclado",
				25,
				new BigDecimal("49990"),
				null
		);
	}

	@Test
	void shouldConsumeValidProductoEvent() {
		assertThatCode(() -> consumer.consumeEvento(event)).doesNotThrowAnyException();
	}

	@Test
	void shouldRejectProductoEventWithoutEventId() {
		event.setEventId(null);

		assertThatThrownBy(() -> consumer.consumeEvento(event))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void shouldRejectProductoEventWithBlankEventType() {
		event.setEventType(" ");

		assertThatThrownBy(() -> consumer.consumeEvento(event))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void shouldRejectProductoEventWithoutProductoId() {
		event.setProductoId(null);

		assertThatThrownBy(() -> consumer.consumeEvento(event))
				.isInstanceOf(IllegalArgumentException.class);
	}

}
