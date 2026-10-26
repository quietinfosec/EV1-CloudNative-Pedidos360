package cl.duoc.pedidos360.pedidos.event;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Properties;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistry;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.EnabledIf;

import com.rabbitmq.client.Channel;

import cl.duoc.pedidos360.pedidos.config.RabbitMQConfig;

/**
 * EP3 (Fase 4): verifica de forma explicita el flujo NACK -> DLQ sobre la cola
 * objetivo del diseno "pedidos360.pedidos.cola".
 *
 * Se registra un listener de prueba que siempre lanza excepcion; tras agotar
 * los reintentos configurados (max-attempts=3) el mensaje debe ser rechazado
 * sin requeue y enrutado al DLX hacia "pedidos360.pedidos.dlq" (x-death).
 */
@SpringBootTest
@ActiveProfiles("local")
@EnabledIf("#{environment.getProperty('messaging.provider','rabbitmq') == 'rabbitmq'}")
class PedidoDlqIntegrationTests {

	@Autowired
	private RabbitTemplate rabbitTemplate;

	@Autowired
	private AmqpAdmin amqpAdmin;

	@Autowired
	private RabbitListenerEndpointRegistry registry;

	private SimpleMessageListenerContainer listenerContainer;

	@BeforeEach
	void setUp() throws InterruptedException {
		listenerContainer = (SimpleMessageListenerContainer)
				registry.getListenerContainer("pedidos-dlq-test-listener");
		listenerContainer.stop();
		amqpAdmin.purgeQueue(RabbitMQConfig.PEDIDOS_COLA, false);
		amqpAdmin.purgeQueue(RabbitMQConfig.PEDIDOS_COLA_DLQ, false);
		amqpAdmin.purgeQueue(RabbitMQConfig.AUDITORIA_COLA, false);
		listenerContainer.start();
		waitForConsumer(listenerContainer);
	}

	@AfterEach
	void tearDown() {
		listenerContainer.stop();
		amqpAdmin.purgeQueue(RabbitMQConfig.PEDIDOS_COLA, false);
		amqpAdmin.purgeQueue(RabbitMQConfig.PEDIDOS_COLA_DLQ, false);
		amqpAdmin.purgeQueue(RabbitMQConfig.AUDITORIA_COLA, false);
	}

	@Test
	void shouldDeadLetterRejectedMessageOnObjetivoQueue() {
		PedidoEvent event = new PedidoEvent(null, 9009L, "ERROR", "usuario-dlq", new BigDecimal("1"), "corr-dlq");

		rabbitTemplate.convertAndSend(
				RabbitMQConfig.TOPIC_EXCHANGE_NAME,
				RabbitMQConfig.topicRoutingKey(RabbitMQConfig.PEDIDO_CREADO),
				event);

		Message deadLetter = rabbitTemplate.receive(RabbitMQConfig.PEDIDOS_COLA_DLQ, 10000);

		assertThat(deadLetter).isNotNull();
		assertThat(deadLetter.getMessageProperties().getHeaders()).containsKey("x-death");
		assertThat(messageCount(RabbitMQConfig.PEDIDOS_COLA)).isZero();
		assertThat(messageCount(RabbitMQConfig.PEDIDOS_COLA_DLQ)).isZero();
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

	/**
	 * Listener de prueba: consume la cola objetivo y siempre falla para forzar
	 * el camino de reintentos y posterior dead-letter. Se declara como bean
	 * adicional solo en el contexto de este test.
	 */
	@TestConfiguration
	static class FailingConsumerConfig {

		@Bean
		FailingPedidosConsumer failingPedidosConsumer() {
			return new FailingPedidosConsumer();
		}
	}

	static class FailingPedidosConsumer {

		@RabbitListener(id = "pedidos-dlq-test-listener", queues = RabbitMQConfig.PEDIDOS_COLA)
		public void consume(PedidoEvent event, Message message, Channel channel) throws IOException {
			try {
				throw new IllegalStateException("Fallo intencional para forzar dead-letter de " + event.getPedidoId());
			} catch (IllegalStateException ex) {
				channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, false);
			}
		}
	}

}
