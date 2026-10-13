package cl.duoc.pedidos360.pedidos.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import cl.duoc.pedidos360.pedidos.config.RabbitMQConfig;

@Component
public class PedidoEventConsumer {

	private static final Logger log = LoggerFactory.getLogger(PedidoEventConsumer.class);

	@RabbitListener(id = "pedidos-eventos-listener", queues = RabbitMQConfig.PEDIDOS_EVENTOS_QUEUE)
	public void consumeEvento(PedidoEvent event) {
		validate(event);
		log.info(
				"Evento RabbitMQ recibido eventId={} eventType={} pedidoId={} correlationId={}",
				event.getEventId(),
				event.getEventType(),
				event.getPedidoId(),
				event.getCorrelationId()
		);
	}

	@RabbitListener(id = "pedidos-errores-listener", queues = RabbitMQConfig.PEDIDOS_ERRORES_QUEUE)
	public void consumeError(PedidoEvent event) {
		validate(event);
		log.warn(
				"Evento de error RabbitMQ recibido eventId={} eventType={} pedidoId={} correlationId={}",
				event.getEventId(),
				event.getEventType(),
				event.getPedidoId(),
				event.getCorrelationId()
		);
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
