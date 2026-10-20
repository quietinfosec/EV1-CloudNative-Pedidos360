package cl.duoc.pedidos360.pedidos.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

import cl.duoc.pedidos360.pedidos.config.RabbitMQConfig;
import cl.duoc.pedidos360.pedidos.event.PedidoEvent;

/**
 * EP3 (4.4): el adaptador SQS serializa el evento y publica en la cola
 * configurada (ElasticMQ en local), sin depender de RabbitMQ.
 */
@ExtendWith(MockitoExtension.class)
class SqsEventPublisherTests {

	@Mock
	private SqsClient sqsClient;

	private SqsEventPublisher publisher;

	private PedidoEvent event;

	@BeforeEach
	void setUp() {
		ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
		publisher = new SqsEventPublisher(sqsClient, objectMapper, "http://localhost:9324/000000000000/pedidos360-eventos");
		event = new PedidoEvent(RabbitMQConfig.PEDIDO_CREADO, 1001L, "CREADO", "usuario-1", new BigDecimal("29990"), "corr-sqs");
		when(sqsClient.sendMessage(any(SendMessageRequest.class))).thenReturn(SendMessageResponse.builder().build());
	}

	@Test
	void publicaElEventoConLaRoutingKeyYElPayloadJson() {
		publisher.publish(RabbitMQConfig.PEDIDO_CREADO, event);

		ArgumentCaptor<SendMessageRequest> captor = ArgumentCaptor.forClass(SendMessageRequest.class);
		verify(sqsClient).sendMessage(captor.capture());

		SendMessageRequest request = captor.getValue();
		assertThat(request.queueUrl()).isEqualTo("http://localhost:9324/000000000000/pedidos360-eventos");
		assertThat(request.messageBody()).contains("\"pedidoId\":1001");
		assertThat(request.messageAttributes().get(SqsEventPublisher.ROUTING_KEY_ATTRIBUTE).stringValue())
				.isEqualTo(RabbitMQConfig.PEDIDO_CREADO);
	}

	@Test
	void publicaUnaVezPorInvocacion() {
		publisher.publish(RabbitMQConfig.PEDIDO_CREADO, event);

		verify(sqsClient, times(1)).sendMessage(any(SendMessageRequest.class));
	}

}
