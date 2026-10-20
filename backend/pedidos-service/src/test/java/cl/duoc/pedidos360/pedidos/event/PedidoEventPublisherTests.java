package cl.duoc.pedidos360.pedidos.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import cl.duoc.pedidos360.pedidos.config.RabbitMQConfig;
import cl.duoc.pedidos360.pedidos.messaging.EventPublisher;

@ExtendWith(MockitoExtension.class)
class PedidoEventPublisherTests {

	@Mock
	private EventPublisher eventPublisher;

	@InjectMocks
	private PedidoEventPublisher publisher;

	private PedidoEvent event;

	@BeforeEach
	void setUp() {
		event = new PedidoEvent(null, 1001L, "CREADO", "usuario-1", new BigDecimal("29990"), "corr-1");
	}

	@Test
	void shouldPublishPedidoCreado() {
		publisher.publishPedidoCreado(event);

		assertThat(event.getEventType()).isEqualTo(RabbitMQConfig.PEDIDO_CREADO);
		verify(eventPublisher).publish(RabbitMQConfig.PEDIDO_CREADO, event);
	}

	@Test
	void shouldPublishPedidoActualizado() {
		publisher.publishPedidoActualizado(event);

		assertThat(event.getEventType()).isEqualTo(RabbitMQConfig.PEDIDO_ACTUALIZADO);
		verify(eventPublisher).publish(RabbitMQConfig.PEDIDO_ACTUALIZADO, event);
	}

	@Test
	void shouldPublishPedidoCancelado() {
		publisher.publishPedidoCancelado(event);

		assertThat(event.getEventType()).isEqualTo(RabbitMQConfig.PEDIDO_CANCELADO);
		verify(eventPublisher).publish(RabbitMQConfig.PEDIDO_CANCELADO, event);
	}

	@Test
	void shouldPublishPedidoError() {
		publisher.publishPedidoError(event);

		assertThat(event.getEventType()).isEqualTo(RabbitMQConfig.PEDIDO_ERROR);
		verify(eventPublisher).publish(RabbitMQConfig.PEDIDO_ERROR, event);
	}

}
