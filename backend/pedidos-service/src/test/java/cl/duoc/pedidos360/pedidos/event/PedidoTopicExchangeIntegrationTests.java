package cl.duoc.pedidos360.pedidos.event;

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

import cl.duoc.pedidos360.pedidos.config.RabbitMQConfig;

/**
 * EP3: verifica el fan-out al Topic Exchange hacia las colas principales
 * "pedidos" y "auditoria" definidas en el diseno objetivo.
 */
@SpringBootTest
@ActiveProfiles("local")
@EnabledIf("#{environment.getProperty('messaging.provider','rabbitmq') == 'rabbitmq'}")
class PedidoTopicExchangeIntegrationTests {

	@Autowired
	private PedidoEventPublisher publisher;

	@Autowired
	private RabbitTemplate rabbitTemplate;

	@Autowired
	private AmqpAdmin amqpAdmin;

	@Autowired
	private ObjectMapper objectMapper;

	@BeforeEach
	void setUp() {
		amqpAdmin.purgeQueue(RabbitMQConfig.PEDIDOS_COLA, false);
		amqpAdmin.purgeQueue(RabbitMQConfig.AUDITORIA_COLA, false);
	}

	@AfterEach
	void tearDown() {
		amqpAdmin.purgeQueue(RabbitMQConfig.PEDIDOS_COLA, false);
		amqpAdmin.purgeQueue(RabbitMQConfig.AUDITORIA_COLA, false);
	}

	@Test
	void shouldFanOutPedidoCreadoToPedidosAndAuditoriaColas() throws Exception {
		PedidoEvent event = new PedidoEvent(
				null,
				2002L,
				"CREADO",
				"usuario-topic",
				new BigDecimal("15000"),
				"corr-topic"
		);

		publisher.publishPedidoCreado(event);

		String expectedTopicKey = RabbitMQConfig.TOPIC_ROUTING_PREFIX + RabbitMQConfig.PEDIDO_CREADO;

		Message enPedidos = rabbitTemplate.receive(RabbitMQConfig.PEDIDOS_COLA, 5000);
		assertThat(enPedidos).isNotNull();
		assertThat(enPedidos.getMessageProperties().getReceivedRoutingKey()).isEqualTo(expectedTopicKey);

		JsonNode payload = objectMapper.readTree(enPedidos.getBody());
		assertThat(payload.get("pedidoId").asLong()).isEqualTo(2002L);
		assertThat(payload.get("eventType").asText()).isEqualTo(RabbitMQConfig.PEDIDO_CREADO);

		Message enAuditoria = rabbitTemplate.receive(RabbitMQConfig.AUDITORIA_COLA, 5000);
		assertThat(enAuditoria).isNotNull();
		assertThat(enAuditoria.getMessageProperties().getReceivedRoutingKey()).isEqualTo(expectedTopicKey);
	}

	@Test
	void shouldNotFanOutPedidoErrorToTopic() {
		PedidoEvent event = new PedidoEvent(
				null,
				3003L,
				"ERROR",
				"usuario-topic",
				new BigDecimal("1"),
				"corr-topic-error"
		);

		publisher.publishPedidoError(event);

		assertThat(rabbitTemplate.receive(RabbitMQConfig.PEDIDOS_COLA, 500)).isNull();
		assertThat(rabbitTemplate.receive(RabbitMQConfig.AUDITORIA_COLA, 500)).isNull();
	}

}
