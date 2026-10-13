package cl.duoc.pedidos360.productos.event;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpException;

import cl.duoc.pedidos360.productos.config.RabbitMQConfig;

@ExtendWith(MockitoExtension.class)
class ProductoDomainEventListenerTests {

	@Mock
	private ProductoEventPublisher publisher;

	@InjectMocks
	private ProductoDomainEventListener listener;

	@Test
	void shouldPublishProductoCreadoAfterCommitCallback() {
		ProductoDomainEvent domainEvent = event(RabbitMQConfig.PRODUCTO_CREADO);

		listener.handle(domainEvent);

		verify(publisher).publishProductoCreado(any(ProductoEvent.class));
	}

	@Test
	void shouldNotPropagateRabbitFailureAfterCommit() {
		ProductoDomainEvent domainEvent = event(RabbitMQConfig.PRODUCTO_CREADO);
		org.mockito.Mockito.doThrow(new AmqpException("broker unavailable"))
				.when(publisher).publishProductoCreado(any(ProductoEvent.class));

		assertThatCode(() -> listener.handle(domainEvent)).doesNotThrowAnyException();
	}

	private ProductoDomainEvent event(String eventType) {
		return new ProductoDomainEvent(eventType, 2001L, "Teclado", 25, new BigDecimal("49990"), null);
	}

}
