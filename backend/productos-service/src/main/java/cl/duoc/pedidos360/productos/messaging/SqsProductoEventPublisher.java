package cl.duoc.pedidos360.productos.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import cl.duoc.pedidos360.productos.config.RabbitMQConfig;
import cl.duoc.pedidos360.productos.event.ProductoEvent;
import cl.duoc.pedidos360.productos.event.ProductoPublisher;

/**
 * EP3 (4.4): adaptador SQS/ElasticMQ del publicador de productos.
 *
 * Se activa con {@code messaging.provider=sqs} en lugar de
 * {@code ProductoEventPublisher} (RabbitMQ). Publica el payload serializado
 * como JSON; la routing key del dominio se preserva dentro del evento
 * ({@code eventType}).
 */
@Component
@ConditionalOnProperty(name = "messaging.provider", havingValue = "sqs")
public class SqsProductoEventPublisher implements ProductoPublisher {

	private static final Logger log = LoggerFactory.getLogger(SqsProductoEventPublisher.class);

	private final SqsClient sqsClient;
	private final ObjectMapper objectMapper;
	private final String queueUrl;

	public SqsProductoEventPublisher(
			SqsClient sqsClient,
			ObjectMapper objectMapper,
			@Value("${messaging.sqs.queue-url:http://localhost:9324/000000000000/pedidos360-eventos}") String queueUrl) {
		this.sqsClient = sqsClient;
		this.objectMapper = objectMapper;
		this.queueUrl = queueUrl;
	}

	@Override
	public void publishProductoCreado(ProductoEvent event) {
		event.setEventType(RabbitMQConfig.PRODUCTO_CREADO);
		publish(event, RabbitMQConfig.PRODUCTO_CREADO);
	}

	@Override
	public void publishProductoActualizado(ProductoEvent event) {
		event.setEventType(RabbitMQConfig.PRODUCTO_ACTUALIZADO);
		publish(event, RabbitMQConfig.PRODUCTO_ACTUALIZADO);
	}

	@Override
	public void publishStockActualizado(ProductoEvent event) {
		event.setEventType(RabbitMQConfig.PRODUCTO_STOCK_ACTUALIZADO);
		publish(event, RabbitMQConfig.PRODUCTO_STOCK_ACTUALIZADO);
	}

	@Override
	public void publishProductoError(ProductoEvent event) {
		event.setEventType(RabbitMQConfig.PRODUCTO_ERROR);
		publish(event, RabbitMQConfig.PRODUCTO_ERROR);
	}

	private void publish(ProductoEvent event, String routingKey) {
		String topicRoutingKey = RabbitMQConfig.TOPIC_ROUTING_PREFIX + routingKey;
		try {
			sqsClient.sendMessage(SendMessageRequest.builder()
					.queueUrl(queueUrl)
					.messageBody(objectMapper.writeValueAsString(event))
					.build());
			log.info(
					"Evento SQS publicado eventId={} eventType={} productoId={} routingKey={} queueUrl={}",
					event.getEventId(),
					event.getEventType(),
					event.getProductoId(),
					topicRoutingKey,
					queueUrl
			);
		} catch (JsonProcessingException exception) {
			throw new IllegalStateException("No fue posible serializar el evento " + routingKey, exception);
		}
	}

}
