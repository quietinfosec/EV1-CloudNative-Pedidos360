package cl.duoc.pedidos360.productos.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import cl.duoc.pedidos360.productos.config.RabbitMQConfig;

@Component
@ConditionalOnProperty(name = "messaging.provider", havingValue = "rabbitmq", matchIfMissing = true)
public class ProductoEventPublisher implements ProductoPublisher {

	private static final Logger log = LoggerFactory.getLogger(ProductoEventPublisher.class);

	private final RabbitTemplate rabbitTemplate;

	public ProductoEventPublisher(RabbitTemplate rabbitTemplate) {
		this.rabbitTemplate = rabbitTemplate;
	}

	@Override
	public void publishProductoCreado(ProductoEvent event) {
		event.setEventType(RabbitMQConfig.PRODUCTO_CREADO);
		publish(event, RabbitMQConfig.PRODUCTO_CREADO);
	}

	@Override
	public void publishProductoActualizado(ProductoEvent event) {
		event.setEventType(RabbitMQConfig.PRODUCTO_ACTUALIZADO);
		publish(event, RabbitMQConfig.PRODUCTO_ACTUALIZADO);
	}

	@Override
	public void publishStockActualizado(ProductoEvent event) {
		event.setEventType(RabbitMQConfig.PRODUCTO_STOCK_ACTUALIZADO);
		publish(event, RabbitMQConfig.PRODUCTO_STOCK_ACTUALIZADO);
	}

	@Override
	public void publishProductoError(ProductoEvent event) {
		event.setEventType(RabbitMQConfig.PRODUCTO_ERROR);
		publish(event, RabbitMQConfig.PRODUCTO_ERROR);
	}

	private void publish(ProductoEvent event, String routingKey) {
		rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_NAME, routingKey, event);
		log.info(
				"Evento RabbitMQ publicado eventId={} eventType={} productoId={} routingKey={} correlationId={}",
				event.getEventId(),
				event.getEventType(),
				event.getProductoId(),
				routingKey,
				event.getCorrelationId()
		);

		// EP3: fan-out al Topic Exchange (dominio.entidad.accion). Los errores
		// permanecen solo en el exchange directo para no contaminar la cola principal.
		if (!RabbitMQConfig.PRODUCTO_ERROR.equals(routingKey)) {
			String topicRoutingKey = RabbitMQConfig.topicRoutingKey(routingKey);
			rabbitTemplate.convertAndSend(RabbitMQConfig.TOPIC_EXCHANGE_NAME, topicRoutingKey, event);
			log.info(
					"Evento RabbitMQ publicado (topic) eventId={} eventType={} productoId={} routingKey={} correlationId={}",
					event.getEventId(),
					event.getEventType(),
					event.getProductoId(),
					topicRoutingKey,
					event.getCorrelationId()
			);
		}
	}

}
