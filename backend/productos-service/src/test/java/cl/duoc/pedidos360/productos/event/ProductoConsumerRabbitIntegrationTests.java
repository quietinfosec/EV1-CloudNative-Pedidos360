package cl.duoc.pedidos360.productos.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.util.Properties;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.MessageListenerContainer;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistry;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.ActiveProfiles;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import cl.duoc.pedidos360.productos.config.RabbitMQConfig;

@SpringBootTest
@ActiveProfiles("local")
class ProductoConsumerRabbitIntegrationTests {

	@Autowired
	private RabbitTemplate rabbitTemplate;

	@Autowired
	private AmqpAdmin amqpAdmin;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private RabbitListenerEndpointRegistry registry;

	@SpyBean
	private ProductoEventConsumer consumer;

	private MessageListenerContainer listenerContainer;

	@BeforeEach
	void setUp() throws InterruptedException {
		listenerContainer = registry.getListenerContainer("productos-eventos-listener");
		listenerContainer.stop();
		amqpAdmin.purgeQueue(RabbitMQConfig.PRODUCTOS_EVENTOS_QUEUE, false);
		amqpAdmin.purgeQueue(RabbitMQConfig.PRODUCTOS_EVENTOS_DLQ, false);
		reset(consumer);
		listenerContainer.start();
		waitForConsumer((SimpleMessageListenerContainer) listenerContainer);
	}

	@AfterEach
	void tearDown() {
		listenerContainer.stop();
		amqpAdmin.purgeQueue(RabbitMQConfig.PRODUCTOS_EVENTOS_QUEUE, false);
		amqpAdmin.purgeQueue(RabbitMQConfig.PRODUCTOS_EVENTOS_DLQ, false);
	}

	@Test
	void shouldConsumeValidProductoEventWithoutDeadLettering() {
		ProductoEvent event = validEvent();

		rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_NAME, RabbitMQConfig.PRODUCTO_CREADO, event);

		verify(consumer, timeout(5000).times(1)).consumeEvento(argThat(received -> received.getEventId().equals(event.getEventId())));
		assertThat(messageCount(RabbitMQConfig.PRODUCTOS_EVENTOS_QUEUE)).isZero();
		assertThat(messageCount(RabbitMQConfig.PRODUCTOS_EVENTOS_DLQ)).isZero();
	}

	@Test
	void shouldRetryThreeTimesThenDeadLetterInvalidProductoEvent() throws Exception {
		ProductoEvent event = validEvent();
		event.setEventType("");

		rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_NAME, RabbitMQConfig.PRODUCTO_CREADO, event);

		verify(consumer, timeout(5000).times(3)).consumeEvento(argThat(received -> received.getEventId().equals(event.getEventId())));
		Message deadLetter = rabbitTemplate.receive(RabbitMQConfig.PRODUCTOS_EVENTOS_DLQ, 5000);

		assertThat(deadLetter).isNotNull();
		assertThat(deadLetter.getMessageProperties().getHeaders()).containsKey("x-death");
		assertThat(messageCount(RabbitMQConfig.PRODUCTOS_EVENTOS_QUEUE)).isZero();

		JsonNode payload = objectMapper.readTree(deadLetter.getBody());
		assertThat(payload.get("eventId").asText()).isEqualTo(event.getEventId().toString());
		assertThat(payload.get("eventType").asText()).isEmpty();
	}

	private ProductoEvent validEvent() {
		return new ProductoEvent(
				RabbitMQConfig.PRODUCTO_CREADO,
				2001L,
				"Teclado",
				25,
				new BigDecimal("49990"),
				null
		);
	}

	private int messageCount(String queueName) {
		Properties properties = amqpAdmin.getQueueProperties(queueName);
		return (Integer) properties.get("QUEUE_MESSAGE_COUNT");
	}

	private void waitForConsumer(SimpleMessageListenerContainer container) throws InterruptedException {
		long deadline = System.currentTimeMillis() + 5000;
		while (container.getActiveConsumerCount() == 0 && System.currentTimeMillis() < deadline) {
			Thread.sleep(50);
		}
		assertThat(container.getActiveConsumerCount()).isGreaterThan(0);
	}

}
