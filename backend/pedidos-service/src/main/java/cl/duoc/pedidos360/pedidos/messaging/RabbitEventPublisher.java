package cl.duoc.pedidos360.pedidos.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import cl.duoc.pedidos360.pedidos.config.RabbitMQConfig;

/**
 * EP3 (4.4): adaptador RabbitMQ del puerto {@link EventPublisher}.
 *
 * Publica al Direct Exchange y, salvo los errores, tambien al Topic Exchange
 * para el fan-out hacia las colas principales y de auditoria.
 */
@Component
@Primary
@ConditionalOnProperty(name = "messaging.provider", havingValue = "rabbitmq", matchIfMissing = true)
public class RabbitEventPublisher implements EventPublisher {

	private static final Logger log = LoggerFactory.getLogger(RabbitEventPublisher.class);

	private final RabbitTemplate rabbitTemplate;

	public RabbitEventPublisher(RabbitTemplate rabbitTemplate) {
		this.rabbitTemplate = rabbitTemplate;
	}

	@Override
	public void publish(String routingKey, Object payload) {
		rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_NAME, routingKey, payload);
		log.info(
				"Evento RabbitMQ publicado exchange={} routingKey={}",
				RabbitMQConfig.EXCHANGE_NAME,
				routingKey
		);

		// EP3: fan-out al Topic Exchange (dominio.entidad.accion). Los errores
		// permanecen solo en el exchange directo para no contaminar la cola principal.
		if (!RabbitMQConfig.PEDIDO_ERROR.equals(routingKey)) {
			String topicRoutingKey = RabbitMQConfig.TOPIC_ROUTING_PREFIX + routingKey;
			rabbitTemplate.convertAndSend(RabbitMQConfig.TOPIC_EXCHANGE_NAME, topicRoutingKey, payload);
			log.info(
					"Evento RabbitMQ publicado (topic) exchange={} routingKey={}",
					RabbitMQConfig.TOPIC_EXCHANGE_NAME,
					topicRoutingKey
			);
		}
	}

}
