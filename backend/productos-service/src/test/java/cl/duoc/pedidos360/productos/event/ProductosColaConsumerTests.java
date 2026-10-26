package cl.duoc.pedidos360.productos.event;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

import com.rabbitmq.client.Channel;

import cl.duoc.pedidos360.productos.config.RabbitMQConfig;

class ProductosColaConsumerTests {

	private static final long DELIVERY_TAG = 12L;

	private ProductosColaConsumer consumer;
	private ProductoEvent event;
	private Channel channel;
	private Message message;

	@BeforeEach
	void setUp() {
		RetryTemplate retryTemplate = new RetryTemplate();
		retryTemplate.setRetryPolicy(new SimpleRetryPolicy(3));
		FixedBackOffPolicy backOffPolicy = new FixedBackOffPolicy();
		backOffPolicy.setBackOffPeriod(1L);
		retryTemplate.setBackOffPolicy(backOffPolicy);

		consumer = new ProductosColaConsumer(new ManualAckRabbitHandler(retryTemplate));
		event = new ProductoEvent(
				RabbitMQConfig.PRODUCTO_CREADO,
				2001L,
				"Teclado",
				25,
				new BigDecimal("49990"),
				null
		);
		channel = mock(Channel.class);
		MessageProperties properties = new MessageProperties();
		properties.setDeliveryTag(DELIVERY_TAG);
		message = new Message(new byte[0], properties);
	}

	@Test
	void shouldAckValidProductoFromTargetQueue() throws IOException {
		consumer.consume(event, message, channel);

		verify(channel).basicAck(DELIVERY_TAG, false);
		verify(channel, never()).basicNack(anyLong(), anyBoolean(), anyBoolean());
	}

	@Test
	void shouldNackInvalidProductoFromTargetQueue() throws IOException {
		event.setEventId(null);

		consumer.consume(event, message, channel);

		verify(channel).basicNack(DELIVERY_TAG, false, false);
		verify(channel, never()).basicAck(anyLong(), anyBoolean());
	}

}
