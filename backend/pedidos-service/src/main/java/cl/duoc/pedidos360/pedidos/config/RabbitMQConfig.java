package cl.duoc.pedidos360.pedidos.config;

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
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * EP3: fuente unica de verdad de la topologia RabbitMQ de pedidos-service.
 *
 * <p>Aqui se declaran exchanges, colas, bindings, DLX/DLQ, conversor JSON,
 * {@link RabbitTemplate} y {@link RetryTemplate}. Ningun productor, consumidor
 * o script de infraestructura duplica estos nombres: todos referencian las
 * constantes de esta clase.</p>
 */
@Configuration
public class RabbitMQConfig {

	public static final String EXCHANGE_NAME = "pedidos360.direct.exchange";
	public static final String DLX_NAME = "pedidos360.dlx";

	// EP3: Topic Exchange para fan-out de eventos por patron (dominio.entidad.accion).
	public static final String TOPIC_EXCHANGE_NAME = "pedidos360.topic.exchange";

	public static final String PEDIDOS_EVENTOS_QUEUE = "pedidos.eventos.queue";
	public static final String PEDIDOS_ERRORES_QUEUE = "pedidos.errores.queue";
	public static final String PEDIDOS_EVENTOS_DLQ = "pedidos.eventos.dlq";
	public static final String PEDIDOS_ERRORES_DLQ = "pedidos.errores.dlq";

	// EP3: tres colas principales del diseno objetivo + DLQ por dominio.
	// pedidos-service declara "pedidos" y "auditoria"; productos-service declara
	// "productos" y "auditoria" (la cola de auditoria es compartida e idempotente).
	public static final String PEDIDOS_COLA = "pedidos360.pedidos.cola";
	public static final String PEDIDOS_COLA_DLQ = "pedidos360.pedidos.dlq";
	public static final String AUDITORIA_COLA = "pedidos360.auditoria.cola";
	public static final String AUDITORIA_COLA_DLQ = "pedidos360.auditoria.dlq";

	public static final String PEDIDO_CREADO = "pedido.creado";
	public static final String PEDIDO_ACTUALIZADO = "pedido.actualizado";
	public static final String PEDIDO_CANCELADO = "pedido.cancelado";
	public static final String PEDIDO_ERROR = "pedido.error";

	public static final String PEDIDOS_EVENTOS_DLQ_ROUTING_KEY = "pedidos.eventos.dlq";
	public static final String PEDIDOS_ERRORES_DLQ_ROUTING_KEY = "pedidos.errores.dlq";

	// EP3: routing keys del Topic Exchange: TOPIC_ROUTING_PREFIX + eventType.
	// El patron de binding es deliberadamente especifico ("pedidos.pedido.#")
	// para no capturar subdominios futuros no previstos; las routing keys
	// publicadas se derivan con topicRoutingKey(...) para garantizar que
	// publicador y binding compartan una unica fuente de verdad.
	public static final String TOPIC_ROUTING_PREFIX = "pedidos.";
	public static final String TOPIC_BINDING_PEDIDOS = "pedidos.pedido.#";
	public static final String TOPIC_BINDING_ALL = "#";

	/**
	 * Construye la routing key canonica del Topic Exchange para un eventType.
	 * Ejemplo: {@code topicRoutingKey("pedido.creado") -> "pedidos.pedido.creado"}.
	 */
	public static String topicRoutingKey(String eventType) {
		return TOPIC_ROUTING_PREFIX + eventType;
	}

	public static final String PEDIDOS_COLA_DLQ_ROUTING_KEY = "pedidos360.pedidos.dlq";
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
	public Queue pedidosEventosQueue() {
		return QueueBuilder.durable(PEDIDOS_EVENTOS_QUEUE)
				.deadLetterExchange(DLX_NAME)
				.deadLetterRoutingKey(PEDIDOS_EVENTOS_DLQ_ROUTING_KEY)
				.build();
	}

	@Bean
	public Queue pedidosErroresQueue() {
		return QueueBuilder.durable(PEDIDOS_ERRORES_QUEUE)
				.deadLetterExchange(DLX_NAME)
				.deadLetterRoutingKey(PEDIDOS_ERRORES_DLQ_ROUTING_KEY)
				.build();
	}

	@Bean
	public Queue pedidosEventosDlq() {
		return QueueBuilder.durable(PEDIDOS_EVENTOS_DLQ).build();
	}

	@Bean
	public Queue pedidosErroresDlq() {
		return QueueBuilder.durable(PEDIDOS_ERRORES_DLQ).build();
	}

	// --- EP3: colas principales + DLQ por dominio ---

	@Bean
	public Queue pedidosCola() {
		return QueueBuilder.durable(PEDIDOS_COLA)
				.deadLetterExchange(DLX_NAME)
				.deadLetterRoutingKey(PEDIDOS_COLA_DLQ_ROUTING_KEY)
				.build();
	}

	@Bean
	public Queue pedidosColaDlq() {
		return QueueBuilder.durable(PEDIDOS_COLA_DLQ).build();
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
	public Binding pedidosColaTopicBinding(TopicExchange pedidos360TopicExchange, Queue pedidosCola) {
		return BindingBuilder.bind(pedidosCola).to(pedidos360TopicExchange).with(TOPIC_BINDING_PEDIDOS);
	}

	@Bean
	public Binding auditoriaColaTopicBinding(TopicExchange pedidos360TopicExchange, Queue auditoriaCola) {
		return BindingBuilder.bind(auditoriaCola).to(pedidos360TopicExchange).with(TOPIC_BINDING_ALL);
	}

	@Bean
	public Binding pedidosColaDlqBinding(DirectExchange pedidos360DeadLetterExchange, Queue pedidosColaDlq) {
		return BindingBuilder.bind(pedidosColaDlq)
				.to(pedidos360DeadLetterExchange)
				.with(PEDIDOS_COLA_DLQ_ROUTING_KEY);
	}

	@Bean
	public Binding auditoriaColaDlqBinding(DirectExchange pedidos360DeadLetterExchange, Queue auditoriaColaDlq) {
		return BindingBuilder.bind(auditoriaColaDlq)
				.to(pedidos360DeadLetterExchange)
				.with(AUDITORIA_COLA_DLQ_ROUTING_KEY);
	}

	// --- bindings existentes ---

	@Bean
	public Binding pedidoCreadoBinding(DirectExchange pedidos360Exchange, Queue pedidosEventosQueue) {
		return BindingBuilder.bind(pedidosEventosQueue).to(pedidos360Exchange).with(PEDIDO_CREADO);
	}

	@Bean
	public Binding pedidoActualizadoBinding(DirectExchange pedidos360Exchange, Queue pedidosEventosQueue) {
		return BindingBuilder.bind(pedidosEventosQueue).to(pedidos360Exchange).with(PEDIDO_ACTUALIZADO);
	}

	@Bean
	public Binding pedidoCanceladoBinding(DirectExchange pedidos360Exchange, Queue pedidosEventosQueue) {
		return BindingBuilder.bind(pedidosEventosQueue).to(pedidos360Exchange).with(PEDIDO_CANCELADO);
	}

	@Bean
	public Binding pedidoErrorBinding(DirectExchange pedidos360Exchange, Queue pedidosErroresQueue) {
		return BindingBuilder.bind(pedidosErroresQueue).to(pedidos360Exchange).with(PEDIDO_ERROR);
	}

	@Bean
	public Binding pedidosEventosDlqBinding(DirectExchange pedidos360DeadLetterExchange, Queue pedidosEventosDlq) {
		return BindingBuilder.bind(pedidosEventosDlq)
				.to(pedidos360DeadLetterExchange)
				.with(PEDIDOS_EVENTOS_DLQ_ROUTING_KEY);
	}

	@Bean
	public Binding pedidosErroresDlqBinding(DirectExchange pedidos360DeadLetterExchange, Queue pedidosErroresDlq) {
		return BindingBuilder.bind(pedidosErroresDlq)
				.to(pedidos360DeadLetterExchange)
				.with(PEDIDOS_ERRORES_DLQ_ROUTING_KEY);
	}

	/**
	 * EP3: reintentos controlados para consumidores con ACK/NACK manual.
	 * Tras 3 intentos fallidos el consumidor hace basicNack(requeue=false) y el
	 * mensaje se enruta a la DLQ declarada en la cola.
	 */
	@Bean
	public RetryTemplate rabbitRetryTemplate() {
		RetryTemplate retryTemplate = new RetryTemplate();
		retryTemplate.setRetryPolicy(new SimpleRetryPolicy(3));
		FixedBackOffPolicy backOffPolicy = new FixedBackOffPolicy();
		backOffPolicy.setBackOffPeriod(500L);
		retryTemplate.setBackOffPolicy(backOffPolicy);
		return retryTemplate;
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
