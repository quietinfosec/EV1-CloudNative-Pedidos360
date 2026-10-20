package cl.duoc.pedidos360.pedidos.event;

import org.springframework.stereotype.Component;

import cl.duoc.pedidos360.pedidos.config.RabbitMQConfig;
import cl.duoc.pedidos360.pedidos.messaging.EventPublisher;

/**
 * EP3 (4.4): fachada de publicacion de eventos de pedido.
 *
 * Solo compone el {@code eventType} de cada evento y delega la entrega al
 * puerto {@link EventPublisher}, cuya implementacion concreta (RabbitMQ o SQS)
 * se selecciona con la propiedad {@code messaging.provider}.
 */
@Component
public class PedidoEventPublisher {

	private final EventPublisher eventPublisher;

	public PedidoEventPublisher(EventPublisher eventPublisher) {
		this.eventPublisher = eventPublisher;
	}

	public void publishPedidoCreado(PedidoEvent event) {
		event.setEventType(RabbitMQConfig.PEDIDO_CREADO);
		eventPublisher.publish(RabbitMQConfig.PEDIDO_CREADO, event);
	}

	public void publishPedidoActualizado(PedidoEvent event) {
		event.setEventType(RabbitMQConfig.PEDIDO_ACTUALIZADO);
		eventPublisher.publish(RabbitMQConfig.PEDIDO_ACTUALIZADO, event);
	}

	public void publishPedidoCancelado(PedidoEvent event) {
		event.setEventType(RabbitMQConfig.PEDIDO_CANCELADO);
		eventPublisher.publish(RabbitMQConfig.PEDIDO_CANCELADO, event);
	}

	public void publishPedidoError(PedidoEvent event) {
		event.setEventType(RabbitMQConfig.PEDIDO_ERROR);
		eventPublisher.publish(RabbitMQConfig.PEDIDO_ERROR, event);
	}

}
