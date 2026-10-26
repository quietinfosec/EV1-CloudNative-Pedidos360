package cl.duoc.pedidos360.productos.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

import com.rabbitmq.client.Channel;

class ManualAckRabbitHandlerTests {

	private static final long DELIVERY_TAG = 43L;

	private ManualAckRabbitHandler handler;
	private Channel channel;
	private Message message;

	@BeforeEach
	void setUp() {
		RetryTemplate retryTemplate = new RetryTemplate();
		retryTemplate.setRetryPolicy(new SimpleRetryPolicy(3));
		FixedBackOffPolicy backOffPolicy = new FixedBackOffPolicy();
		backOffPolicy.setBackOffPeriod(1L);
		retryTemplate.setBackOffPolicy(backOffPolicy);

		handler = new ManualAckRabbitHandler(retryTemplate);
		channel = mock(Channel.class);
		MessageProperties properties = new MessageProperties();
		properties.setDeliveryTag(DELIVERY_TAG);
		message = new Message(new byte[0], properties);
	}

	@Test
	void shouldAckWhenActionSucceeds() throws IOException {
		handler.handle("test", message, channel, () -> {
		});

		verify(channel).basicAck(DELIVERY_TAG, false);
		verify(channel, never()).basicNack(anyLong(), anyBoolean(), anyBoolean());
	}

	@Test
	void shouldRetryThreeTimesThenNackWhenActionFails() throws IOException {
		AtomicInteger attempts = new AtomicInteger();

		handler.handle("test", message, channel, () -> {
			attempts.incrementAndGet();
			throw new IllegalStateException("fallo intencional");
		});

		assertThat(attempts).hasValue(3);
		verify(channel).basicNack(DELIVERY_TAG, false, false);
		verify(channel, never()).basicAck(anyLong(), anyBoolean());
	}

}
