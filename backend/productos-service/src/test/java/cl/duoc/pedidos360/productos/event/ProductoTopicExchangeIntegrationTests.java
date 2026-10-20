package cl.duoc.pedidos360.productos.event;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.EnabledIf;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import cl.duoc.pedidos360.productos.config.RabbitMQConfig;

/**
 * EP3: verifica el fan-out al Topic Exchange hacia las colas principales
 * "productos" y "auditoria" definidas en el diseno objetivo.
 */
@SpringBootTest
@ActiveProfiles("local")
@EnabledIf("#{environment.getProperty('messaging.provider','rabbitmq') == 'rabbitmq'}")
class ProductoTopicExchangeIntegrationTests {

	@Autowired
	private ProductoEventPublisher publisher;

	@Autowired
	private RabbitTemplate rabbitTemplate;

	@Autowired
	private AmqpAdmin amqpAdmin;

	@Autowired
	private ObjectMapper objectMapper;

	@BeforeEach
	void setUp() {
		amqpAdmin.purgeQueue(RabbitMQConfig.PRODUCTOS_COLA, false);
		amqpAdmin.purgeQueue(RabbitMQConfig.AUDITORIA_COLA, false);
	}

	@AfterEach
	void tearDown() {
		amqpAdmin.purgeQueue(RabbitMQConfig.PRODUCTOS_COLA, false);
		amqpAdmin.purgeQueue(RabbitMQConfig.AUDITORIA_COLA, false);
	}

	@Test
	void shouldFanOutProductoCreadoToProductosAndAuditoriaColas() throws Exception {
		ProductoEvent event = new ProductoEvent(
				null,
				4004L,
				"Teclado mecanico",
				25,
				new BigDecimal("45990"),
				"corr-topic-producto"
		);

		publisher.publishProductoCreado(event);

		String expectedTopicKey = RabbitMQConfig.TOPIC_ROUTING_PREFIX + RabbitMQConfig.PRODUCTO_CREADO;

		Message enProductos = rabbitTemplate.receive(RabbitMQConfig.PRODUCTOS_COLA, 5000);
		assertThat(enProductos).isNotNull();
		assertThat(enProductos.getMessageProperties().getReceivedRoutingKey()).isEqualTo(expectedTopicKey);

		JsonNode payload = objectMapper.readTree(enProductos.getBody());
		assertThat(payload.get("productoId").asLong()).isEqualTo(4004L);
		assertThat(payload.get("eventType").asText()).isEqualTo(RabbitMQConfig.PRODUCTO_CREADO);

		Message enAuditoria = rabbitTemplate.receive(RabbitMQConfig.AUDITORIA_COLA, 5000);
		assertThat(enAuditoria).isNotNull();
		assertThat(enAuditoria.getMessageProperties().getReceivedRoutingKey()).isEqualTo(expectedTopicKey);
	}

	@Test
	void shouldFanOutStockActualizadoToTopic() {
		ProductoEvent event = new ProductoEvent(
				null,
				5005L,
				"Mouse inalambrico",
				10,
				new BigDecimal("19990"),
				"corr-topic-stock"
		);

		publisher.publishStockActualizado(event);

		Message enProductos = rabbitTemplate.receive(RabbitMQConfig.PRODUCTOS_COLA, 5000);
		assertThat(enProductos).isNotNull();
		assertThat(enProductos.getMessageProperties().getReceivedRoutingKey())
				.isEqualTo(RabbitMQConfig.TOPIC_ROUTING_PREFIX + RabbitMQConfig.PRODUCTO_STOCK_ACTUALIZADO);
	}

	@Test
	void shouldNotFanOutProductoErrorToTopic() {
		ProductoEvent event = new ProductoEvent(
				null,
				6006L,
				"Producto invalido",
				0,
				new BigDecimal("0"),
				"corr-topic-error"
		);

		publisher.publishProductoError(event);

		assertThat(rabbitTemplate.receive(RabbitMQConfig.PRODUCTOS_COLA, 500)).isNull();
		assertThat(rabbitTemplate.receive(RabbitMQConfig.AUDITORIA_COLA, 500)).isNull();
	}

}
