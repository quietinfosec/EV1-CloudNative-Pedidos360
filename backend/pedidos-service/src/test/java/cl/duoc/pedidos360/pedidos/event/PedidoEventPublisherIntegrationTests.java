package cl.duoc.pedidos360.pedidos.event;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.EnabledIf;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import cl.duoc.pedidos360.pedidos.config.RabbitMQConfig;

@SpringBootTest
@ActiveProfiles("local")
@EnabledIf("#{environment.getProperty('messaging.provider','rabbitmq') == 'rabbitmq'}")
class PedidoEventPublisherIntegrationTests {

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
		amqpAdmin.purgeQueue(RabbitMQConfig.PEDIDOS_EVENTOS_QUEUE, false);
	}

	@AfterEach
	void tearDown() {
		amqpAdmin.purgeQueue(RabbitMQConfig.PEDIDOS_EVENTOS_QUEUE, false);
	}

	@Test
	void shouldPublishPedidoCreadoAsJsonToRealQueue() throws Exception {
		PedidoEvent event = new PedidoEvent(
				null,
				1001L,
				"CREADO",
				"usuario-1",
				new BigDecimal("29990"),
				"corr-pedido-real"
		);

		publisher.publishPedidoCreado(event);

		Message message = rabbitTemplate.receive(RabbitMQConfig.PEDIDOS_EVENTOS_QUEUE, 5000);
		assertThat(message).isNotNull();
		assertThat(message.getMessageProperties().getContentType()).isEqualTo(MessageProperties.CONTENT_TYPE_JSON);

		JsonNode payload = objectMapper.readTree(message.getBody());
		assertThat(payload.get("eventId").asText()).isEqualTo(event.getEventId().toString());
		assertThat(payload.get("eventType").asText()).isEqualTo(RabbitMQConfig.PEDIDO_CREADO);
		assertThat(payload.get("pedidoId").asLong()).isEqualTo(1001L);
	}

}
