package cl.duoc.pedidos360.productos.event;

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

import cl.duoc.pedidos360.productos.config.RabbitMQConfig;

/**
 * EP3 (Fase 4): verifica de forma explicita el flujo NACK -> DLQ sobre la cola
 * objetivo del diseno "pedidos360.productos.cola".
 *
 * Se registra un listener de prueba que siempre lanza excepcion; tras agotar
 * los reintentos configurados (max-attempts=3) el mensaje debe ser rechazado
 * sin requeue y enrutado al DLX hacia "pedidos360.productos.dlq" (x-death).
 */
@SpringBootTest
@ActiveProfiles("local")
@EnabledIf("#{environment.getProperty('messaging.provider','rabbitmq') == 'rabbitmq'}")
class ProductoDlqIntegrationTests {

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
				registry.getListenerContainer("productos-dlq-test-listener");
		listenerContainer.stop();
		amqpAdmin.purgeQueue(RabbitMQConfig.PRODUCTOS_COLA, false);
		amqpAdmin.purgeQueue(RabbitMQConfig.PRODUCTOS_COLA_DLQ, false);
		amqpAdmin.purgeQueue(RabbitMQConfig.AUDITORIA_COLA, false);
		listenerContainer.start();
		waitForConsumer(listenerContainer);
	}

	@AfterEach
	void tearDown() {
		listenerContainer.stop();
		amqpAdmin.purgeQueue(RabbitMQConfig.PRODUCTOS_COLA, false);
		amqpAdmin.purgeQueue(RabbitMQConfig.PRODUCTOS_COLA_DLQ, false);
		amqpAdmin.purgeQueue(RabbitMQConfig.AUDITORIA_COLA, false);
	}

	@Test
	void shouldDeadLetterRejectedMessageOnObjetivoQueue() {
		ProductoEvent event = new ProductoEvent(null, 9090L, "Invalido", 0, new BigDecimal("0"), "corr-dlq");

		rabbitTemplate.convertAndSend(
				RabbitMQConfig.TOPIC_EXCHANGE_NAME,
				RabbitMQConfig.topicRoutingKey(RabbitMQConfig.PRODUCTO_CREADO),
				event);

		Message deadLetter = rabbitTemplate.receive(RabbitMQConfig.PRODUCTOS_COLA_DLQ, 10000);

		assertThat(deadLetter).isNotNull();
		assertThat(deadLetter.getMessageProperties().getHeaders()).containsKey("x-death");
		assertThat(messageCount(RabbitMQConfig.PRODUCTOS_COLA)).isZero();
		assertThat(messageCount(RabbitMQConfig.PRODUCTOS_COLA_DLQ)).isZero();
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

	@TestConfiguration
	static class FailingConsumerConfig {

		@Bean
		FailingProductosConsumer failingProductosConsumer() {
			return new FailingProductosConsumer();
		}
	}

	static class FailingProductosConsumer {

		@RabbitListener(id = "productos-dlq-test-listener", queues = RabbitMQConfig.PRODUCTOS_COLA)
		public void consume(ProductoEvent event, Message message, Channel channel) throws IOException {
			try {
				throw new IllegalStateException("Fallo intencional para forzar dead-letter de " + event.getProductoId());
			} catch (IllegalStateException ex) {
				channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, false);
			}
		}
	}

}
