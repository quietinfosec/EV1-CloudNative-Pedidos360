package cl.duoc.pedidos360.productos.event;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.rabbitmq.client.Channel;

import cl.duoc.pedidos360.productos.config.RabbitMQConfig;

@Component
@ConditionalOnProperty(name = "messaging.provider", havingValue = "rabbitmq", matchIfMissing = true)
public class ProductoEventConsumer {

	private static final Logger log = LoggerFactory.getLogger(ProductoEventConsumer.class);

	private final ManualAckRabbitHandler ackHandler;

	public ProductoEventConsumer(ManualAckRabbitHandler ackHandler) {
		this.ackHandler = ackHandler;
	}

	@RabbitListener(id = "productos-eventos-listener", queues = RabbitMQConfig.PRODUCTOS_EVENTOS_QUEUE)
	public void consumeEvento(ProductoEvent event, Message message, Channel channel) throws IOException {
		ackHandler.handle("productos-eventos", message, channel, () -> {
			validate(event);
			log.info(
					"Evento RabbitMQ recibido eventId={} eventType={} productoId={} correlationId={}",
					event.getEventId(),
					event.getEventType(),
					event.getProductoId(),
					event.getCorrelationId()
			);
		});
	}

	@RabbitListener(id = "productos-errores-listener", queues = RabbitMQConfig.PRODUCTOS_ERRORES_QUEUE)
	public void consumeError(ProductoEvent event, Message message, Channel channel) throws IOException {
		ackHandler.handle("productos-errores", message, channel, () -> {
			validate(event);
			log.warn(
					"Evento de error RabbitMQ recibido eventId={} eventType={} productoId={} correlationId={}",
					event.getEventId(),
					event.getEventType(),
					event.getProductoId(),
					event.getCorrelationId()
			);
		});
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
