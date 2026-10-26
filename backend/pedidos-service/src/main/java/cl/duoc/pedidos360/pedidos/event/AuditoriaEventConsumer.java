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
 * EP3: modulo de procesamiento de auditoria.
 *
 * Consume la cola principal "auditoria" (tercera cola del diseno objetivo),
 * suscrita al Topic Exchange con el patron "#". Recibe eventos de todos los
 * dominios, por lo que trabaja con el {@link Message} crudo en lugar de un DTO
 * fuertemente tipado (los payloads de pedidos y productos difieren).
 */
@Component
@ConditionalOnProperty(name = "messaging.provider", havingValue = "rabbitmq", matchIfMissing = true)
public class AuditoriaEventConsumer {

	private static final Logger log = LoggerFactory.getLogger(AuditoriaEventConsumer.class);

	private final ManualAckRabbitHandler ackHandler;

	public AuditoriaEventConsumer(ManualAckRabbitHandler ackHandler) {
		this.ackHandler = ackHandler;
	}

	@RabbitListener(id = "auditoria-listener", queues = RabbitMQConfig.AUDITORIA_COLA)
	public void consume(Message message, Channel channel) throws IOException {
		ackHandler.handle("auditoria", message, channel, () -> {
			if (message == null) {
				throw new IllegalArgumentException("Message no puede ser null");
			}
			log.info(
					"Evento de auditoria recibido routingKey={} contentType={} bytes={}",
					message.getMessageProperties().getReceivedRoutingKey(),
					message.getMessageProperties().getContentType(),
					message.getBody() != null ? message.getBody().length : 0
			);
		});
	}

}
