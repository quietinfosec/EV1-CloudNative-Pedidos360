package cl.duoc.pedidos360.pedidos.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
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

	public static final String PEDIDOS_EVENTOS_QUEUE = "pedidos.eventos.queue";
	public static final String PEDIDOS_ERRORES_QUEUE = "pedidos.errores.queue";
	public static final String PEDIDOS_EVENTOS_DLQ = "pedidos.eventos.dlq";
	public static final String PEDIDOS_ERRORES_DLQ = "pedidos.errores.dlq";

	public static final String PEDIDO_CREADO = "pedido.creado";
	public static final String PEDIDO_ACTUALIZADO = "pedido.actualizado";
	public static final String PEDIDO_CANCELADO = "pedido.cancelado";
	public static final String PEDIDO_ERROR = "pedido.error";

	public static final String PEDIDOS_EVENTOS_DLQ_ROUTING_KEY = "pedidos.eventos.dlq";
	public static final String PEDIDOS_ERRORES_DLQ_ROUTING_KEY = "pedidos.errores.dlq";

	@Bean
	public DirectExchange pedidos360Exchange() {
		return new DirectExchange(EXCHANGE_NAME, true, false);
	}

	@Bean
	public DirectExchange pedidos360DeadLetterExchange() {
		return new DirectExchange(DLX_NAME, true, false);
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
