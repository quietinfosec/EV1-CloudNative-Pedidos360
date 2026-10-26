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

/**
 * EP3: consumidor productivo de la cola principal de productos.
 *
 * La cola {@code pedidos360.productos.cola} esta enlazada al Topic Exchange con
 * el patron {@code productos.producto.#}. A diferencia de las colas legadas,
 * esta es la cola del diseno objetivo y confirma/rechaza cada mensaje de forma
 * explicita.
 */
@Component
@ConditionalOnProperty(name = "messaging.provider", havingValue = "rabbitmq", matchIfMissing = true)
public class ProductosColaConsumer {

	private static final Logger log = LoggerFactory.getLogger(ProductosColaConsumer.class);

	private final ManualAckRabbitHandler ackHandler;

	public ProductosColaConsumer(ManualAckRabbitHandler ackHandler) {
		this.ackHandler = ackHandler;
	}

	@RabbitListener(id = "productos-cola-listener", queues = RabbitMQConfig.PRODUCTOS_COLA)
	public void consume(ProductoEvent event, Message message, Channel channel) throws IOException {
		ackHandler.handle("productos-cola", message, channel, () -> {
			validate(event);
			log.info(
					"Producto procesado desde cola principal eventId={} eventType={} productoId={} correlationId={}",
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
