package cl.duoc.pedidos360.productos.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.ObjectMapper;

@Configuration
public class RabbitMQConfig {

	public static final String EXCHANGE_NAME = "pedidos360.direct.exchange";
	public static final String DLX_NAME = "pedidos360.dlx";

	// EP3: Topic Exchange para fan-out de eventos por patron (dominio.entidad.accion).
	public static final String TOPIC_EXCHANGE_NAME = "pedidos360.topic.exchange";

	public static final String PRODUCTOS_EVENTOS_QUEUE = "productos.eventos.queue";
	public static final String PRODUCTOS_ERRORES_QUEUE = "productos.errores.queue";
	public static final String PRODUCTOS_EVENTOS_DLQ = "productos.eventos.dlq";
	public static final String PRODUCTOS_ERRORES_DLQ = "productos.errores.dlq";

	// EP3: colas principales del diseno objetivo + DLQ por dominio.
	// productos-service declara "productos" y "auditoria"; "auditoria" es
	// compartida con pedidos-service (declaracion idempotente).
	public static final String PRODUCTOS_COLA = "pedidos360.productos.cola";
	public static final String PRODUCTOS_COLA_DLQ = "pedidos360.productos.dlq";
	public static final String AUDITORIA_COLA = "pedidos360.auditoria.cola";
	public static final String AUDITORIA_COLA_DLQ = "pedidos360.auditoria.dlq";

	public static final String PRODUCTO_CREADO = "producto.creado";
	public static final String PRODUCTO_ACTUALIZADO = "producto.actualizado";
	public static final String PRODUCTO_STOCK_ACTUALIZADO = "producto.stock.actualizado";
	public static final String PRODUCTO_ERROR = "producto.error";

	public static final String PRODUCTOS_EVENTOS_DLQ_ROUTING_KEY = "productos.eventos.dlq";
	public static final String PRODUCTOS_ERRORES_DLQ_ROUTING_KEY = "productos.errores.dlq";

	// EP3: routing keys del Topic Exchange: TOPIC_ROUTING_PREFIX + eventType.
	// El patron de binding es deliberadamente especifico ("productos.producto.#")
	// para no capturar subdominios futuros no previstos; las routing keys
	// publicadas se derivan con topicRoutingKey(...) para garantizar que
	// publicador y binding compartan una unica fuente de verdad.
	public static final String TOPIC_ROUTING_PREFIX = "productos.";
	public static final String TOPIC_BINDING_PRODUCTOS = "productos.producto.#";
	public static final String TOPIC_BINDING_ALL = "#";

	/**
	 * Construye la routing key canonica del Topic Exchange para un eventType.
	 * Ejemplo: {@code topicRoutingKey("producto.creado") -> "productos.producto.creado"}.
	 */
	public static String topicRoutingKey(String eventType) {
		return TOPIC_ROUTING_PREFIX + eventType;
	}

	public static final String PRODUCTOS_COLA_DLQ_ROUTING_KEY = "pedidos360.productos.dlq";
	public static final String AUDITORIA_COLA_DLQ_ROUTING_KEY = "pedidos360.auditoria.dlq";

	@Bean
	public DirectExchange pedidos360Exchange() {
		return new DirectExchange(EXCHANGE_NAME, true, false);
	}

	@Bean
	public DirectExchange pedidos360DeadLetterExchange() {
		return new DirectExchange(DLX_NAME, true, false);
	}

	@Bean
	public TopicExchange pedidos360TopicExchange() {
		return new TopicExchange(TOPIC_EXCHANGE_NAME, true, false);
	}

	@Bean
	public Queue productosEventosQueue() {
		return QueueBuilder.durable(PRODUCTOS_EVENTOS_QUEUE)
				.deadLetterExchange(DLX_NAME)
				.deadLetterRoutingKey(PRODUCTOS_EVENTOS_DLQ_ROUTING_KEY)
				.build();
	}

	@Bean
	public Queue productosErroresQueue() {
		return QueueBuilder.durable(PRODUCTOS_ERRORES_QUEUE)
				.deadLetterExchange(DLX_NAME)
				.deadLetterRoutingKey(PRODUCTOS_ERRORES_DLQ_ROUTING_KEY)
				.build();
	}

	@Bean
	public Queue productosEventosDlq() {
		return QueueBuilder.durable(PRODUCTOS_EVENTOS_DLQ).build();
	}

	@Bean
	public Queue productosErroresDlq() {
		return QueueBuilder.durable(PRODUCTOS_ERRORES_DLQ).build();
	}

	// --- EP3: colas principales + DLQ por dominio ---

	@Bean
	public Queue productosCola() {
		return QueueBuilder.durable(PRODUCTOS_COLA)
				.deadLetterExchange(DLX_NAME)
				.deadLetterRoutingKey(PRODUCTOS_COLA_DLQ_ROUTING_KEY)
				.build();
	}

	@Bean
	public Queue productosColaDlq() {
		return QueueBuilder.durable(PRODUCTOS_COLA_DLQ).build();
	}

	@Bean
	public Queue auditoriaCola() {
		return QueueBuilder.durable(AUDITORIA_COLA)
				.deadLetterExchange(DLX_NAME)
				.deadLetterRoutingKey(AUDITORIA_COLA_DLQ_ROUTING_KEY)
				.build();
	}

	@Bean
	public Queue auditoriaColaDlq() {
		return QueueBuilder.durable(AUDITORIA_COLA_DLQ).build();
	}

	// EP3: bindings al Topic Exchange.
	@Bean
	public Binding productosColaTopicBinding(TopicExchange pedidos360TopicExchange, Queue productosCola) {
		return BindingBuilder.bind(productosCola).to(pedidos360TopicExchange).with(TOPIC_BINDING_PRODUCTOS);
	}

	@Bean
	public Binding auditoriaColaTopicBinding(TopicExchange pedidos360TopicExchange, Queue auditoriaCola) {
		return BindingBuilder.bind(auditoriaCola).to(pedidos360TopicExchange).with(TOPIC_BINDING_ALL);
	}

	@Bean
	public Binding productosColaDlqBinding(DirectExchange pedidos360DeadLetterExchange, Queue productosColaDlq) {
		return BindingBuilder.bind(productosColaDlq)
				.to(pedidos360DeadLetterExchange)
				.with(PRODUCTOS_COLA_DLQ_ROUTING_KEY);
	}

	@Bean
	public Binding auditoriaColaDlqBinding(DirectExchange pedidos360DeadLetterExchange, Queue auditoriaColaDlq) {
		return BindingBuilder.bind(auditoriaColaDlq)
				.to(pedidos360DeadLetterExchange)
				.with(AUDITORIA_COLA_DLQ_ROUTING_KEY);
	}

	// --- bindings existentes ---

	@Bean
	public Binding productoCreadoBinding(DirectExchange pedidos360Exchange, Queue productosEventosQueue) {
		return BindingBuilder.bind(productosEventosQueue).to(pedidos360Exchange).with(PRODUCTO_CREADO);
	}

	@Bean
	public Binding productoActualizadoBinding(DirectExchange pedidos360Exchange, Queue productosEventosQueue) {
		return BindingBuilder.bind(productosEventosQueue).to(pedidos360Exchange).with(PRODUCTO_ACTUALIZADO);
	}

	@Bean
	public Binding productoStockActualizadoBinding(DirectExchange pedidos360Exchange, Queue productosEventosQueue) {
		return BindingBuilder.bind(productosEventosQueue).to(pedidos360Exchange).with(PRODUCTO_STOCK_ACTUALIZADO);
	}

	@Bean
	public Binding productoErrorBinding(DirectExchange pedidos360Exchange, Queue productosErroresQueue) {
		return BindingBuilder.bind(productosErroresQueue).to(pedidos360Exchange).with(PRODUCTO_ERROR);
	}

	@Bean
	public Binding productosEventosDlqBinding(DirectExchange pedidos360DeadLetterExchange, Queue productosEventosDlq) {
		return BindingBuilder.bind(productosEventosDlq)
				.to(pedidos360DeadLetterExchange)
				.with(PRODUCTOS_EVENTOS_DLQ_ROUTING_KEY);
	}

	@Bean
	public Binding productosErroresDlqBinding(DirectExchange pedidos360DeadLetterExchange, Queue productosErroresDlq) {
		return BindingBuilder.bind(productosErroresDlq)
				.to(pedidos360DeadLetterExchange)
				.with(PRODUCTOS_ERRORES_DLQ_ROUTING_KEY);
	}

	@Bean
	public MessageConverter messageConverter(ObjectMapper objectMapper) {
		return new Jackson2JsonMessageConverter(objectMapper);
	}

	@Bean
	public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter messageConverter) {
		RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
		rabbitTemplate.setMessageConverter(messageConverter);
		return rabbitTemplate;
	}

}
