package cl.duoc.pedidos360.productos.config;

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

	public static final String PRODUCTOS_EVENTOS_QUEUE = "productos.eventos.queue";
	public static final String PRODUCTOS_ERRORES_QUEUE = "productos.errores.queue";
	public static final String PRODUCTOS_EVENTOS_DLQ = "productos.eventos.dlq";
	public static final String PRODUCTOS_ERRORES_DLQ = "productos.errores.dlq";

	public static final String PRODUCTO_CREADO = "producto.creado";
	public static final String PRODUCTO_ACTUALIZADO = "producto.actualizado";
	public static final String PRODUCTO_STOCK_ACTUALIZADO = "producto.stock.actualizado";
	public static final String PRODUCTO_ERROR = "producto.error";

	public static final String PRODUCTOS_EVENTOS_DLQ_ROUTING_KEY = "productos.eventos.dlq";
	public static final String PRODUCTOS_ERRORES_DLQ_ROUTING_KEY = "productos.errores.dlq";

	@Bean
	public DirectExchange pedidos360Exchange() {
		return new DirectExchange(EXCHANGE_NAME, true, false);
	}

	@Bean
	public DirectExchange pedidos360DeadLetterExchange() {
		return new DirectExchange(DLX_NAME, true, false);
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
