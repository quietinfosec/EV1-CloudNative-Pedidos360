package cl.duoc.pedidos360.pedidos.messaging;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

/**
 * EP3 (4.4): adaptador SQS/ElasticMQ del puerto {@link EventPublisher}.
 *
 * Se activa con {@code messaging.provider=sqs}. Publica el payload serializado
 * como JSON y expone la routing key como atributo del mensaje. En local apunta
 * a ElasticMQ (endpoint configurable).
 */
@Component
@ConditionalOnProperty(name = "messaging.provider", havingValue = "sqs")
public class SqsEventPublisher implements EventPublisher {

	public static final String ROUTING_KEY_ATTRIBUTE = "routingKey";

	private static final Logger log = LoggerFactory.getLogger(SqsEventPublisher.class);

	private final SqsClient sqsClient;
	private final ObjectMapper objectMapper;
	private final String queueUrl;

	public SqsEventPublisher(
			SqsClient sqsClient,
			ObjectMapper objectMapper,
			@Value("${messaging.sqs.queue-url:http://localhost:9324/000000000000/pedidos360-eventos}") String queueUrl) {
		this.sqsClient = sqsClient;
		this.objectMapper = objectMapper;
		this.queueUrl = queueUrl;
	}

	@Override
	public void publish(String routingKey, Object payload) {
		try {
			String body = objectMapper.writeValueAsString(payload);
			sqsClient.sendMessage(SendMessageRequest.builder()
					.queueUrl(queueUrl)
					.messageBody(body)
					.messageAttributes(Map.of(ROUTING_KEY_ATTRIBUTE, MessageAttributeValue.builder()
							.dataType("String")
							.stringValue(routingKey)
							.build()))
					.build());
			log.info("Evento SQS publicado routingKey={} queueUrl={}", routingKey, queueUrl);
		} catch (JsonProcessingException exception) {
			throw new IllegalStateException("No fue posible serializar el evento " + routingKey, exception);
		}
	}

}
