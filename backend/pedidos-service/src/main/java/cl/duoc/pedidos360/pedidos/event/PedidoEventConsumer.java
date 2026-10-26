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

@Component
@ConditionalOnProperty(name = "messaging.provider", havingValue = "rabbitmq", matchIfMissing = true)
public class PedidoEventConsumer {

	private static final Logger log = LoggerFactory.getLogger(PedidoEventConsumer.class);

	private final ManualAckRabbitHandler ackHandler;

	public PedidoEventConsumer(ManualAckRabbitHandler ackHandler) {
		this.ackHandler = ackHandler;
	}

	@RabbitListener(id = "pedidos-eventos-listener", queues = RabbitMQConfig.PEDIDOS_EVENTOS_QUEUE)
	public void consumeEvento(PedidoEvent event, Message message, Channel channel) throws IOException {
		ackHandler.handle("pedidos-eventos", message, channel, () -> {
			validate(event);
			log.info(
					"Evento RabbitMQ recibido eventId={} eventType={} pedidoId={} correlationId={}",
					event.getEventId(),
					event.getEventType(),
					event.getPedidoId(),
					event.getCorrelationId()
			);
		});
	}

	@RabbitListener(id = "pedidos-errores-listener", queues = RabbitMQConfig.PEDIDOS_ERRORES_QUEUE)
	public void consumeError(PedidoEvent event, Message message, Channel channel) throws IOException {
		ackHandler.handle("pedidos-errores", message, channel, () -> {
			validate(event);
			log.warn(
					"Evento de error RabbitMQ recibido eventId={} eventType={} pedidoId={} correlationId={}",
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
