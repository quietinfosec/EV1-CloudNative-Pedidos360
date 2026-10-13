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
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import cl.duoc.pedidos360.pedidos.config.RabbitMQConfig;

@ExtendWith(MockitoExtension.class)
class PedidoEventPublisherTests {

	@Mock
	private RabbitTemplate rabbitTemplate;

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
		verify(rabbitTemplate).convertAndSend(RabbitMQConfig.EXCHANGE_NAME, RabbitMQConfig.PEDIDO_CREADO, event);
	}

	@Test
	void shouldPublishPedidoActualizado() {
		publisher.publishPedidoActualizado(event);

		assertThat(event.getEventType()).isEqualTo(RabbitMQConfig.PEDIDO_ACTUALIZADO);
		verify(rabbitTemplate).convertAndSend(RabbitMQConfig.EXCHANGE_NAME, RabbitMQConfig.PEDIDO_ACTUALIZADO, event);
	}

	@Test
	void shouldPublishPedidoCancelado() {
		publisher.publishPedidoCancelado(event);

		assertThat(event.getEventType()).isEqualTo(RabbitMQConfig.PEDIDO_CANCELADO);
		verify(rabbitTemplate).convertAndSend(RabbitMQConfig.EXCHANGE_NAME, RabbitMQConfig.PEDIDO_CANCELADO, event);
	}

	@Test
	void shouldPublishPedidoError() {
		publisher.publishPedidoError(event);

		assertThat(event.getEventType()).isEqualTo(RabbitMQConfig.PEDIDO_ERROR);
		verify(rabbitTemplate).convertAndSend(RabbitMQConfig.EXCHANGE_NAME, RabbitMQConfig.PEDIDO_ERROR, event);
	}

}
