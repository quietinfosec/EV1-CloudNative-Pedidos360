package cl.duoc.pedidos360.productos.messaging;

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

import cl.duoc.pedidos360.productos.config.RabbitMQConfig;
import cl.duoc.pedidos360.productos.event.ProductoEvent;

/**
 * EP3 (4.4): el adaptador SQS serializa el evento y publica en la cola
 * configurada (ElasticMQ en local), sin depender de RabbitMQ.
 */
@ExtendWith(MockitoExtension.class)
class SqsProductoEventPublisherTests {

	@Mock
	private SqsClient sqsClient;

	private SqsProductoEventPublisher publisher;

	private ProductoEvent event;

	@BeforeEach
	void setUp() {
		ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
		publisher = new SqsProductoEventPublisher(sqsClient, objectMapper, "http://localhost:9324/000000000000/pedidos360-eventos");
		event = new ProductoEvent(null, 2001L, "Teclado", 25, new BigDecimal("49990"), "corr-sqs");
		when(sqsClient.sendMessage(any(SendMessageRequest.class))).thenReturn(SendMessageResponse.builder().build());
	}

	@Test
	void publicaElEventoConElPayloadJson() {
		publisher.publishProductoCreado(event);

		ArgumentCaptor<SendMessageRequest> captor = ArgumentCaptor.forClass(SendMessageRequest.class);
		verify(sqsClient).sendMessage(captor.capture());

		SendMessageRequest request = captor.getValue();
		assertThat(request.queueUrl()).isEqualTo("http://localhost:9324/000000000000/pedidos360-eventos");
		assertThat(request.messageBody()).contains("\"productoId\":2001");
		assertThat(request.messageBody()).contains(RabbitMQConfig.PRODUCTO_CREADO);
	}

	@Test
	void publicaUnaVezPorInvocacion() {
		publisher.publishStockActualizado(event);

		verify(sqsClient, times(1)).sendMessage(any(SendMessageRequest.class));
	}

}
