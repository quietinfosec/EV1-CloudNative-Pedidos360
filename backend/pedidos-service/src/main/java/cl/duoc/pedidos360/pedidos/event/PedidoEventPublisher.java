package cl.duoc.pedidos360.pedidos.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import cl.duoc.pedidos360.pedidos.config.RabbitMQConfig;

@Component
public class PedidoEventPublisher {

	private static final Logger log = LoggerFactory.getLogger(PedidoEventPublisher.class);

	private final RabbitTemplate rabbitTemplate;

	public PedidoEventPublisher(RabbitTemplate rabbitTemplate) {
		this.rabbitTemplate = rabbitTemplate;
	}

	public void publishPedidoCreado(PedidoEvent event) {
		event.setEventType(RabbitMQConfig.PEDIDO_CREADO);
		publish(event, RabbitMQConfig.PEDIDO_CREADO);
	}

	public void publishPedidoActualizado(PedidoEvent event) {
		event.setEventType(RabbitMQConfig.PEDIDO_ACTUALIZADO);
		publish(event, RabbitMQConfig.PEDIDO_ACTUALIZADO);
	}

	public void publishPedidoCancelado(PedidoEvent event) {
		event.setEventType(RabbitMQConfig.PEDIDO_CANCELADO);
		publish(event, RabbitMQConfig.PEDIDO_CANCELADO);
	}

	public void publishPedidoError(PedidoEvent event) {
		event.setEventType(RabbitMQConfig.PEDIDO_ERROR);
		publish(event, RabbitMQConfig.PEDIDO_ERROR);
	}

	private void publish(PedidoEvent event, String routingKey) {
		rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_NAME, routingKey, event);
		log.info(
				"Evento RabbitMQ publicado eventId={} eventType={} pedidoId={} routingKey={} correlationId={}",
				event.getEventId(),
				event.getEventType(),
				event.getPedidoId(),
				routingKey,
				event.getCorrelationId()
		);
	}

}
