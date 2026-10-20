package cl.duoc.pedidos360.pedidos.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import cl.duoc.pedidos360.pedidos.config.RabbitMQConfig;
import cl.duoc.pedidos360.pedidos.messaging.EventPublisher;

@Component
public class PedidoDomainEventListener {

	private static final Logger log = LoggerFactory.getLogger(PedidoDomainEventListener.class);

	private final EventPublisher publisher;

	public PedidoDomainEventListener(EventPublisher publisher) {
		this.publisher = publisher;
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void handle(PedidoDomainEvent domainEvent) {
		PedidoEvent event = toIntegrationEvent(domainEvent);
		String routingKey = domainEvent.getEventType();

		log.info(
				"Publicando evento RabbitMQ after-commit eventId={} eventType={} pedidoId={} routingKey={}",
				event.getEventId(),
				event.getEventType(),
				event.getPedidoId(),
				routingKey
		);

		try {
			publish(event, routingKey);
		} catch (RuntimeException exception) {
			log.error(
					"No fue posible publicar evento after-commit eventId={} eventType={} pedidoId={} routingKey={}",
					event.getEventId(),
					event.getEventType(),
					event.getPedidoId(),
					routingKey,
					exception
			);
		}
	}

	private PedidoEvent toIntegrationEvent(PedidoDomainEvent domainEvent) {
		return new PedidoEvent(
				domainEvent.getEventType(),
				domainEvent.getPedidoId(),
				domainEvent.getEstado(),
				domainEvent.getUsuarioId(),
				domainEvent.getTotal(),
				domainEvent.getCorrelationId()
		);
	}

	private void publish(PedidoEvent event, String routingKey) {
		switch (routingKey) {
			case RabbitMQConfig.PEDIDO_CREADO, RabbitMQConfig.PEDIDO_ACTUALIZADO,
					RabbitMQConfig.PEDIDO_CANCELADO -> publisher.publish(routingKey, event);
			default -> throw new IllegalArgumentException("Routing key de pedido no soportada: " + routingKey);
		}
	}

}
