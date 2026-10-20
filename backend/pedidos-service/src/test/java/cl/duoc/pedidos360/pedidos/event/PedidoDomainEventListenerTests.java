package cl.duoc.pedidos360.pedidos.event;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpException;

import cl.duoc.pedidos360.pedidos.config.RabbitMQConfig;
import cl.duoc.pedidos360.pedidos.messaging.EventPublisher;

@ExtendWith(MockitoExtension.class)
class PedidoDomainEventListenerTests {

	@Mock
	private EventPublisher publisher;

	@InjectMocks
	private PedidoDomainEventListener listener;

	@Test
	void shouldPublishPedidoCreadoAfterCommitCallback() {
		PedidoDomainEvent domainEvent = event(RabbitMQConfig.PEDIDO_CREADO);

		listener.handle(domainEvent);

		verify(publisher).publish(eq(RabbitMQConfig.PEDIDO_CREADO), any(PedidoEvent.class));
	}

	@Test
	void shouldNotPropagateRabbitFailureAfterCommit() {
		PedidoDomainEvent domainEvent = event(RabbitMQConfig.PEDIDO_CREADO);
		org.mockito.Mockito.doThrow(new AmqpException("broker unavailable"))
				.when(publisher).publish(eq(RabbitMQConfig.PEDIDO_CREADO), any(PedidoEvent.class));

		assertThatCode(() -> listener.handle(domainEvent)).doesNotThrowAnyException();
	}

	private PedidoDomainEvent event(String eventType) {
		return new PedidoDomainEvent(eventType, 1001L, "PENDIENTE", "usuario-1", new BigDecimal("29990"), null);
	}

}
