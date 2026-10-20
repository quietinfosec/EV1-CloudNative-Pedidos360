package cl.duoc.pedidos360.productos.event;

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

import cl.duoc.pedidos360.productos.config.RabbitMQConfig;
import cl.duoc.pedidos360.productos.dto.ProductoRequest;
import cl.duoc.pedidos360.productos.dto.ProductoResponse;
import cl.duoc.pedidos360.productos.repository.ProductoRepository;
import cl.duoc.pedidos360.productos.service.ProductoService;

@SpringBootTest
@ActiveProfiles("local")
@EnabledIf("#{environment.getProperty('messaging.provider','rabbitmq') == 'rabbitmq'}")
class ProductoServiceRabbitIntegrationTests {

	@Autowired
	private ProductoService productoService;

	@Autowired
	private ProductoRepository productoRepository;

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
		productoRepository.deleteAll();
		amqpAdmin.purgeQueue(RabbitMQConfig.PRODUCTOS_EVENTOS_QUEUE, false);
	}

	@AfterEach
	void tearDown() {
		productoRepository.deleteAll();
		amqpAdmin.purgeQueue(RabbitMQConfig.PRODUCTOS_EVENTOS_QUEUE, false);
	}

	@Test
	void shouldPublishProductoCreadoAfterServiceTransactionCommit() throws Exception {
		ProductoResponse response = productoService.crear(request());

		Message message = rabbitTemplate.receive(RabbitMQConfig.PRODUCTOS_EVENTOS_QUEUE, 5000);
		assertThat(message).isNotNull();
		assertThat(message.getMessageProperties().getContentType()).isEqualTo(MessageProperties.CONTENT_TYPE_JSON);

		JsonNode payload = objectMapper.readTree(message.getBody());
		assertThat(payload.get("eventType").asText()).isEqualTo(RabbitMQConfig.PRODUCTO_CREADO);
		assertThat(payload.get("productoId").asLong()).isEqualTo(response.getId());
		assertThat(payload.get("nombre").asText()).isEqualTo(response.getNombre());
		assertThat(payload.get("stock").asInt()).isEqualTo(response.getStock());
		assertThat(payload.get("precio").decimalValue()).isEqualByComparingTo(response.getPrecio());
	}

	@Test
	void shouldPublishOnlyProductoActualizadoWhenStockUnchanged() throws Exception {
		ProductoResponse created = productoService.crear(request());
		drainProductoCreado();

		productoService.actualizar(
				created.getId(),
				new ProductoRequest("Teclado Pro", "Mecanico RGB", new BigDecimal("59990"), 25)
		);

		Message message = rabbitTemplate.receive(RabbitMQConfig.PRODUCTOS_EVENTOS_QUEUE, 5000);
		assertThat(message).isNotNull();

		JsonNode payload = objectMapper.readTree(message.getBody());
		assertThat(payload.get("eventType").asText()).isEqualTo(RabbitMQConfig.PRODUCTO_ACTUALIZADO);
		assertThat(payload.get("productoId").asLong()).isEqualTo(created.getId());
		assertThat(payload.get("stock").asInt()).isEqualTo(25);

		assertThat(rabbitTemplate.receive(RabbitMQConfig.PRODUCTOS_EVENTOS_QUEUE, 300)).isNull();
	}

	@Test
	void shouldPublishOnlyStockActualizadoWhenStockChanges() throws Exception {
		ProductoResponse created = productoService.crear(request());
		drainProductoCreado();

		productoService.actualizar(
				created.getId(),
				new ProductoRequest("Teclado", "Mecanico", new BigDecimal("49990"), 7)
		);

		Message message = rabbitTemplate.receive(RabbitMQConfig.PRODUCTOS_EVENTOS_QUEUE, 5000);
		assertThat(message).isNotNull();

		JsonNode payload = objectMapper.readTree(message.getBody());
		assertThat(payload.get("eventType").asText()).isEqualTo(RabbitMQConfig.PRODUCTO_STOCK_ACTUALIZADO);
		assertThat(payload.get("productoId").asLong()).isEqualTo(created.getId());
		assertThat(payload.get("stock").asInt()).isEqualTo(7);

		assertThat(rabbitTemplate.receive(RabbitMQConfig.PRODUCTOS_EVENTOS_QUEUE, 300)).isNull();
	}

	@Test
	void shouldNotPublishProductoCreadoWhenOuterTransactionRollsBack() {
		TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);

		assertThatThrownBy(() -> transactionTemplate.executeWithoutResult(status -> {
			productoService.crear(request());
			throw new IllegalStateException("rollback requested");
		})).isInstanceOf(IllegalStateException.class);

		assertThat(productoRepository.count()).isZero();
		assertThat(rabbitTemplate.receive(RabbitMQConfig.PRODUCTOS_EVENTOS_QUEUE, 500)).isNull();
	}

	private ProductoRequest request() {
		return new ProductoRequest("Teclado", "Mecanico", new BigDecimal("49990"), 25);
	}

	private void drainProductoCreado() throws Exception {
		Message createdEvent = rabbitTemplate.receive(RabbitMQConfig.PRODUCTOS_EVENTOS_QUEUE, 5000);
		assertThat(createdEvent).isNotNull();
		assertThat(objectMapper.readTree(createdEvent.getBody()).get("eventType").asText())
				.isEqualTo(RabbitMQConfig.PRODUCTO_CREADO);
	}

}
