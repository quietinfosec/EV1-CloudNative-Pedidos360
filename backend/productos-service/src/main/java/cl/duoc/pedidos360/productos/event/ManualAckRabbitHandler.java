package cl.duoc.pedidos360.productos.event;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;

import com.rabbitmq.client.Channel;

/**
 * EP3: ACK/NACK manual con reintentos controlados.
 *
 * <p>El listener delega aqui el procesamiento. Si la accion termina bien se
 * confirma el mensaje con {@code basicAck}. Si falla tras los reintentos se
 * rechaza sin reencolar con {@code basicNack(requeue=false)} y RabbitMQ lo
 * enruta a la DLQ configurada en la cola.</p>
 */
@Component
public class ManualAckRabbitHandler {

	private static final Logger log = LoggerFactory.getLogger(ManualAckRabbitHandler.class);

	private final RetryTemplate retryTemplate;

	public ManualAckRabbitHandler(RetryTemplate retryTemplate) {
		this.retryTemplate = retryTemplate;
	}

	public void handle(String consumer, Message message, Channel channel, Runnable action) throws IOException {
		if (message == null) {
			throw new IllegalArgumentException("Message no puede ser null");
		}
		long deliveryTag = message.getMessageProperties().getDeliveryTag();
		try {
			retryTemplate.execute(context -> {
				action.run();
				return null;
			});
		} catch (Exception ex) {
			log.error(
					"consumer={} fallo tras reintentos; enviando a DLQ deliveryTag={} routingKey={} causa={}",
					consumer,
					deliveryTag,
					message.getMessageProperties().getReceivedRoutingKey(),
					ex.getMessage(),
					ex
			);
			channel.basicNack(deliveryTag, false, false);
			return;
		}
		channel.basicAck(deliveryTag, false);
		log.debug("consumer={} ACK deliveryTag={}", consumer, deliveryTag);
	}

}
