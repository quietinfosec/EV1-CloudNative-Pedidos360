package cl.duoc.pedidos360.pedidos.event;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.rabbitmq.client.Channel;

import cl.duoc.pedidos360.pedidos.config.RabbitMQConfig;

/**
 * EP3: consumidor productivo de la cola principal de pedidos.
 *
 * La cola {@code pedidos360.pedidos.cola} esta enlazada al Topic Exchange con
 * el patron {@code pedidos.pedido.#}. A diferencia de las colas legadas, esta
 * es la cola del diseno objetivo y confirma/rechaza cada mensaje de forma
 * explicita.
 */
@Component
@ConditionalOnProperty(name = "messaging.provider", havingValue = "rabbitmq", matchIfMissing = true)
public class PedidosColaConsumer {

	private static final Logger log = LoggerFactory.getLogger(PedidosColaConsumer.class);

	private final ManualAckRabbitHandler ackHandler;

	public PedidosColaConsumer(ManualAckRabbitHandler ackHandler) {
		this.ackHandler = ackHandler;
	}

	@RabbitListener(id = "pedidos-cola-listener", queues = RabbitMQConfig.PEDIDOS_COLA)
	public void consume(PedidoEvent event, Message message, Channel channel) throws IOException {
		ackHandler.handle("pedidos-cola", message, channel, () -> {
			validate(event);
			log.info(
					"Pedido procesado desde cola principal eventId={} eventType={} pedidoId={} correlationId={}",
					event.getEventId(),
					event.getEventType(),
					event.getPedidoId(),
					event.getCorrelationId()
			);
		});
	}

	private void validate(PedidoEvent event) {
		if (event == null) {
			throw new IllegalArgumentException("PedidoEvent no puede ser null");
		}
		if (event.getEventId() == null) {
			throw new IllegalArgumentException("PedidoEvent.eventId es obligatorio");
		}
		if (event.getEventType() == null || event.getEventType().isBlank()) {
			throw new IllegalArgumentException("PedidoEvent.eventType es obligatorio");
		}
		if (event.getPedidoId() == null) {
			throw new IllegalArgumentException("PedidoEvent.pedidoId es obligatorio");
		}
	}

}
