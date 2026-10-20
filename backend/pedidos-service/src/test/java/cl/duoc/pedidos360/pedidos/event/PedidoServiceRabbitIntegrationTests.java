package cl.duoc.pedidos360.pedidos.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import cl.duoc.pedidos360.pedidos.config.RabbitMQConfig;
import cl.duoc.pedidos360.pedidos.dto.PedidoRequest;
import cl.duoc.pedidos360.pedidos.dto.PedidoResponse;
import cl.duoc.pedidos360.pedidos.entity.EstadoPedido;
import cl.duoc.pedidos360.pedidos.repository.PedidoRepository;
import cl.duoc.pedidos360.pedidos.service.PedidoService;

@SpringBootTest
@ActiveProfiles("local")
@EnabledIf("#{environment.getProperty('messaging.provider','rabbitmq') == 'rabbitmq'}")
class PedidoServiceRabbitIntegrationTests {

	@Autowired
	private PedidoService pedidoService;

	@Autowired
	private PedidoRepository pedidoRepository;

	@Autowired
	private RabbitTemplate rabbitTemplate;

	@Autowired
	private AmqpAdmin amqpAdmin;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private PlatformTransactionManager transactionManager;

	@BeforeEach
	void setUp() {
		pedidoRepository.deleteAll();
		amqpAdmin.purgeQueue(RabbitMQConfig.PEDIDOS_EVENTOS_QUEUE, false);
	}

	@AfterEach
	void tearDown() {
		pedidoRepository.deleteAll();
		amqpAdmin.purgeQueue(RabbitMQConfig.PEDIDOS_EVENTOS_QUEUE, false);
	}

	@Test
	void shouldPublishPedidoCreadoAfterServiceTransactionCommit() throws Exception {
		PedidoResponse response = pedidoService.crear(request());

		Message message = rabbitTemplate.receive(RabbitMQConfig.PEDIDOS_EVENTOS_QUEUE, 5000);
		assertThat(message).isNotNull();
		assertThat(message.getMessageProperties().getContentType()).isEqualTo(MessageProperties.CONTENT_TYPE_JSON);

		JsonNode payload = objectMapper.readTree(message.getBody());
		assertThat(payload.get("eventType").asText()).isEqualTo(RabbitMQConfig.PEDIDO_CREADO);
		assertThat(payload.get("pedidoId").asLong()).isEqualTo(response.getId());
		assertThat(payload.get("estado").asText()).isEqualTo(response.getEstado().name());
		assertThat(payload.get("usuarioId").asText()).isEqualTo(response.getUsuario());
		assertThat(payload.get("total").decimalValue()).isEqualByComparingTo(response.getTotal());
	}

	@Test
	void shouldPublishPedidoActualizadoAfterServiceTransactionCommit() throws Exception {
		PedidoResponse created = pedidoService.crear(request());
		drainPedidoCreado();

		PedidoResponse updated = pedidoService.actualizar(
				created.getId(),
				new PedidoRequest("bob@duoc.cl", new BigDecimal("15000"), EstadoPedido.CONFIRMADO)
		);

		Message message = rabbitTemplate.receive(RabbitMQConfig.PEDIDOS_EVENTOS_QUEUE, 5000);
		assertThat(message).isNotNull();

		JsonNode payload = objectMapper.readTree(message.getBody());
		assertThat(payload.get("eventType").asText()).isEqualTo(RabbitMQConfig.PEDIDO_ACTUALIZADO);
		assertThat(payload.get("pedidoId").asLong()).isEqualTo(updated.getId());
		assertThat(payload.get("estado").asText()).isEqualTo(EstadoPedido.CONFIRMADO.name());
		assertThat(payload.get("total").decimalValue()).isEqualByComparingTo(new BigDecimal("15000"));
	}

	@Test
	void shouldPublishPedidoCanceladoAfterServiceTransactionCommit() throws Exception {
		PedidoResponse created = pedidoService.crear(request());
		drainPedidoCreado();

		pedidoService.cancelar(created.getId());

		Message message = rabbitTemplate.receive(RabbitMQConfig.PEDIDOS_EVENTOS_QUEUE, 5000);
		assertThat(message).isNotNull();

		JsonNode payload = objectMapper.readTree(message.getBody());
		assertThat(payload.get("eventType").asText()).isEqualTo(RabbitMQConfig.PEDIDO_CANCELADO);
		assertThat(payload.get("pedidoId").asLong()).isEqualTo(created.getId());
		assertThat(payload.get("estado").asText()).isEqualTo(EstadoPedido.CANCELADO.name());
	}

	@Test
	void shouldNotPublishPedidoCreadoWhenOuterTransactionRollsBack() {
		TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);

		assertThatThrownBy(() -> transactionTemplate.executeWithoutResult(status -> {
			pedidoService.crear(request());
			throw new IllegalStateException("rollback requested");
		})).isInstanceOf(IllegalStateException.class);

		assertThat(pedidoRepository.count()).isZero();
		assertThat(rabbitTemplate.receive(RabbitMQConfig.PEDIDOS_EVENTOS_QUEUE, 500)).isNull();
	}

	private PedidoRequest request() {
		return new PedidoRequest("ana@duoc.cl", new BigDecimal("29990"), EstadoPedido.PENDIENTE);
	}

	private void drainPedidoCreado() throws Exception {
		Message createdEvent = rabbitTemplate.receive(RabbitMQConfig.PEDIDOS_EVENTOS_QUEUE, 5000);
		assertThat(createdEvent).isNotNull();
		assertThat(objectMapper.readTree(createdEvent.getBody()).get("eventType").asText())
				.isEqualTo(RabbitMQConfig.PEDIDO_CREADO);
	}

}
