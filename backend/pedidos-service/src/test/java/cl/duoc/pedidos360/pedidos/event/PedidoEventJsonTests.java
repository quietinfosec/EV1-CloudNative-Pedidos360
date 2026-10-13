package cl.duoc.pedidos360.pedidos.event;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;

import cl.duoc.pedidos360.pedidos.config.RabbitMQConfig;

class PedidoEventJsonTests {

	private final ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules().build();

	@Test
	void shouldSerializePedidoEventToJson() throws Exception {
		PedidoEvent event = new PedidoEvent(
				RabbitMQConfig.PEDIDO_CREADO,
				1001L,
				"CREADO",
				"usuario-1",
				new BigDecimal("29990.50"),
				"corr-1"
		);

		String json = objectMapper.writeValueAsString(event);
		JsonNode root = objectMapper.readTree(json);

		assertThat(root.get("eventId").asText()).isEqualTo(event.getEventId().toString());
		assertThat(root.get("eventType").asText()).isEqualTo(RabbitMQConfig.PEDIDO_CREADO);
		assertThat(root.get("eventVersion").asInt()).isEqualTo(1);
		assertThat(root.hasNonNull("occurredAt")).isTrue();
		assertThat(root.get("pedidoId").asLong()).isEqualTo(1001L);
		assertThat(root.get("total").decimalValue()).isEqualByComparingTo("29990.50");
		assertThat(root.get("correlationId").asText()).isEqualTo("corr-1");

		PedidoEvent deserialized = objectMapper.readValue(json, PedidoEvent.class);
		assertThat(deserialized.getOccurredAt()).isEqualTo(event.getOccurredAt());
	}

}
