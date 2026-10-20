package cl.duoc.pedidos360.pedidos.event;

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
import org.springframework.amqp.rabbit.listener.MessageListenerContainer;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistry;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.EnabledIf;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import cl.duoc.pedidos360.pedidos.config.RabbitMQConfig;

@SpringBootTest
@ActiveProfiles("local")
@EnabledIf("#{environment.getProperty('messaging.provider','rabbitmq') == 'rabbitmq'}")
class PedidoConsumerRabbitIntegrationTests {

	@Autowired
	private RabbitTemplate rabbitTemplate;

	@Autowired
	private AmqpAdmin amqpAdmin;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private RabbitListenerEndpointRegistry registry;

	@SpyBean
	private PedidoEventConsumer consumer;

	private MessageListenerContainer listenerContainer;

	@BeforeEach
	void setUp() throws InterruptedException {
		listenerContainer = registry.getListenerContainer("pedidos-eventos-listener");
		listenerContainer.stop();
		amqpAdmin.purgeQueue(RabbitMQConfig.PEDIDOS_EVENTOS_QUEUE, false);
		amqpAdmin.purgeQueue(RabbitMQConfig.PEDIDOS_EVENTOS_DLQ, false);
		reset(consumer);
		listenerContainer.start();
		waitForConsumer((SimpleMessageListenerContainer) listenerContainer);
	}

	@AfterEach
	void tearDown() {
		listenerContainer.stop();
		amqpAdmin.purgeQueue(RabbitMQConfig.PEDIDOS_EVENTOS_QUEUE, false);
		amqpAdmin.purgeQueue(RabbitMQConfig.PEDIDOS_EVENTOS_DLQ, false);
	}

	@Test
	void shouldConsumeValidPedidoEventWithoutDeadLettering() {
		PedidoEvent event = validEvent();

		rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_NAME, RabbitMQConfig.PEDIDO_CREADO, event);

		verify(consumer, timeout(5000).times(1)).consumeEvento(argThat(received -> received.getEventId().equals(event.getEventId())));
		assertThat(messageCount(RabbitMQConfig.PEDIDOS_EVENTOS_QUEUE)).isZero();
		assertThat(messageCount(RabbitMQConfig.PEDIDOS_EVENTOS_DLQ)).isZero();
	}

	@Test
	void shouldRetryThreeTimesThenDeadLetterInvalidPedidoEvent() throws Exception {
		PedidoEvent event = validEvent();
		event.setEventType("");

		rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_NAME, RabbitMQConfig.PEDIDO_CREADO, event);

		verify(consumer, timeout(5000).times(3)).consumeEvento(argThat(received -> received.getEventId().equals(event.getEventId())));
		Message deadLetter = rabbitTemplate.receive(RabbitMQConfig.PEDIDOS_EVENTOS_DLQ, 5000);

		assertThat(deadLetter).isNotNull();
		assertThat(deadLetter.getMessageProperties().getHeaders()).containsKey("x-death");
		assertThat(messageCount(RabbitMQConfig.PEDIDOS_EVENTOS_QUEUE)).isZero();

		JsonNode payload = objectMapper.readTree(deadLetter.getBody());
		assertThat(payload.get("eventId").asText()).isEqualTo(event.getEventId().toString());
		assertThat(payload.get("eventType").asText()).isEmpty();
	}

	private PedidoEvent validEvent() {
		return new PedidoEvent(
				RabbitMQConfig.PEDIDO_CREADO,
				1001L,
				"PENDIENTE",
				"usuario-1",
				new BigDecimal("29990"),
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
