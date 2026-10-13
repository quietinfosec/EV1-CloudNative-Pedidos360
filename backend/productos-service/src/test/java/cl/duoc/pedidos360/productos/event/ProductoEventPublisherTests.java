package cl.duoc.pedidos360.productos.event;

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

import cl.duoc.pedidos360.productos.config.RabbitMQConfig;

@ExtendWith(MockitoExtension.class)
class ProductoEventPublisherTests {

	@Mock
	private RabbitTemplate rabbitTemplate;

	@InjectMocks
	private ProductoEventPublisher publisher;

	private ProductoEvent event;

	@BeforeEach
	void setUp() {
		event = new ProductoEvent(null, 2001L, "Teclado", 25, new BigDecimal("49990"), "corr-2");
	}

	@Test
	void shouldPublishProductoCreado() {
		publisher.publishProductoCreado(event);

		assertThat(event.getEventType()).isEqualTo(RabbitMQConfig.PRODUCTO_CREADO);
		verify(rabbitTemplate).convertAndSend(RabbitMQConfig.EXCHANGE_NAME, RabbitMQConfig.PRODUCTO_CREADO, event);
	}

	@Test
	void shouldPublishProductoActualizado() {
		publisher.publishProductoActualizado(event);

		assertThat(event.getEventType()).isEqualTo(RabbitMQConfig.PRODUCTO_ACTUALIZADO);
		verify(rabbitTemplate).convertAndSend(RabbitMQConfig.EXCHANGE_NAME, RabbitMQConfig.PRODUCTO_ACTUALIZADO, event);
	}

	@Test
	void shouldPublishStockActualizado() {
		publisher.publishStockActualizado(event);

		assertThat(event.getEventType()).isEqualTo(RabbitMQConfig.PRODUCTO_STOCK_ACTUALIZADO);
		verify(rabbitTemplate).convertAndSend(RabbitMQConfig.EXCHANGE_NAME, RabbitMQConfig.PRODUCTO_STOCK_ACTUALIZADO, event);
	}

	@Test
	void shouldPublishProductoError() {
		publisher.publishProductoError(event);

		assertThat(event.getEventType()).isEqualTo(RabbitMQConfig.PRODUCTO_ERROR);
		verify(rabbitTemplate).convertAndSend(RabbitMQConfig.EXCHANGE_NAME, RabbitMQConfig.PRODUCTO_ERROR, event);
	}

}
