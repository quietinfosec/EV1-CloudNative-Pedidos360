package cl.duoc.pedidos360.pedidos.event;

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

import cl.duoc.pedidos360.pedidos.config.RabbitMQConfig;

class PedidosColaConsumerTests {

	private static final long DELIVERY_TAG = 11L;

	private PedidosColaConsumer consumer;
	private PedidoEvent event;
	private Channel channel;
	private Message message;

	@BeforeEach
	void setUp() {
		RetryTemplate retryTemplate = new RetryTemplate();
		retryTemplate.setRetryPolicy(new SimpleRetryPolicy(3));
		FixedBackOffPolicy backOffPolicy = new FixedBackOffPolicy();
		backOffPolicy.setBackOffPeriod(1L);
		retryTemplate.setBackOffPolicy(backOffPolicy);

		consumer = new PedidosColaConsumer(new ManualAckRabbitHandler(retryTemplate));
		event = new PedidoEvent(
				RabbitMQConfig.PEDIDO_CREADO,
				1001L,
				"PENDIENTE",
				"usuario-1",
				new BigDecimal("29990"),
				null
		);
		channel = mock(Channel.class);
		MessageProperties properties = new MessageProperties();
		properties.setDeliveryTag(DELIVERY_TAG);
		message = new Message(new byte[0], properties);
	}

	@Test
	void shouldAckValidPedidoFromTargetQueue() throws IOException {
		consumer.consume(event, message, channel);

		verify(channel).basicAck(DELIVERY_TAG, false);
		verify(channel, never()).basicNack(anyLong(), anyBoolean(), anyBoolean());
	}

	@Test
	void shouldNackInvalidPedidoFromTargetQueue() throws IOException {
		event.setEventId(null);

		consumer.consume(event, message, channel);

		verify(channel).basicNack(DELIVERY_TAG, false, false);
		verify(channel, never()).basicAck(anyLong(), anyBoolean());
	}

}
