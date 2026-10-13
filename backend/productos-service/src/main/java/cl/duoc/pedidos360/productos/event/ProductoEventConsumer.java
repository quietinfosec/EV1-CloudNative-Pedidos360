package cl.duoc.pedidos360.productos.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import cl.duoc.pedidos360.productos.config.RabbitMQConfig;

@Component
public class ProductoEventConsumer {

	private static final Logger log = LoggerFactory.getLogger(ProductoEventConsumer.class);

	@RabbitListener(id = "productos-eventos-listener", queues = RabbitMQConfig.PRODUCTOS_EVENTOS_QUEUE)
	public void consumeEvento(ProductoEvent event) {
		validate(event);
		log.info(
				"Evento RabbitMQ recibido eventId={} eventType={} productoId={} correlationId={}",
				event.getEventId(),
				event.getEventType(),
				event.getProductoId(),
				event.getCorrelationId()
		);
	}

	@RabbitListener(id = "productos-errores-listener", queues = RabbitMQConfig.PRODUCTOS_ERRORES_QUEUE)
	public void consumeError(ProductoEvent event) {
		validate(event);
		log.warn(
				"Evento de error RabbitMQ recibido eventId={} eventType={} productoId={} correlationId={}",
				event.getEventId(),
				event.getEventType(),
				event.getProductoId(),
				event.getCorrelationId()
		);
	}

	private void validate(ProductoEvent event) {
		if (event == null) {
			throw new IllegalArgumentException("ProductoEvent no puede ser null");
		}
		if (event.getEventId() == null) {
			throw new IllegalArgumentException("ProductoEvent.eventId es obligatorio");
		}
		if (event.getEventType() == null || event.getEventType().isBlank()) {
			throw new IllegalArgumentException("ProductoEvent.eventType es obligatorio");
		}
		if (event.getProductoId() == null) {
			throw new IllegalArgumentException("ProductoEvent.productoId es obligatorio");
		}
	}

}
