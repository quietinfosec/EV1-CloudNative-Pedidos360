package cl.duoc.pedidos360.productos.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import cl.duoc.pedidos360.productos.config.RabbitMQConfig;

@Component
public class ProductoEventPublisher {

	private static final Logger log = LoggerFactory.getLogger(ProductoEventPublisher.class);

	private final RabbitTemplate rabbitTemplate;

	public ProductoEventPublisher(RabbitTemplate rabbitTemplate) {
		this.rabbitTemplate = rabbitTemplate;
	}

	public void publishProductoCreado(ProductoEvent event) {
		event.setEventType(RabbitMQConfig.PRODUCTO_CREADO);
		publish(event, RabbitMQConfig.PRODUCTO_CREADO);
	}

	public void publishProductoActualizado(ProductoEvent event) {
		event.setEventType(RabbitMQConfig.PRODUCTO_ACTUALIZADO);
		publish(event, RabbitMQConfig.PRODUCTO_ACTUALIZADO);
	}

	public void publishStockActualizado(ProductoEvent event) {
		event.setEventType(RabbitMQConfig.PRODUCTO_STOCK_ACTUALIZADO);
		publish(event, RabbitMQConfig.PRODUCTO_STOCK_ACTUALIZADO);
	}

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
	}

}
