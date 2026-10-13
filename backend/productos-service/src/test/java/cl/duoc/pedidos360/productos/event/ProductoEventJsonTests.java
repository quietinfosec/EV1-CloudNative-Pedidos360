package cl.duoc.pedidos360.productos.event;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;

import cl.duoc.pedidos360.productos.config.RabbitMQConfig;

class ProductoEventJsonTests {

	private final ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules().build();

	@Test
	void shouldSerializeProductoEventToJson() throws Exception {
		ProductoEvent event = new ProductoEvent(
				RabbitMQConfig.PRODUCTO_ACTUALIZADO,
				2001L,
				"Teclado",
				25,
				new BigDecimal("49990.50"),
				"corr-2"
		);

		String json = objectMapper.writeValueAsString(event);
		JsonNode root = objectMapper.readTree(json);

		assertThat(root.get("eventId").asText()).isEqualTo(event.getEventId().toString());
		assertThat(root.get("eventType").asText()).isEqualTo(RabbitMQConfig.PRODUCTO_ACTUALIZADO);
		assertThat(root.get("eventVersion").asInt()).isEqualTo(1);
		assertThat(root.hasNonNull("occurredAt")).isTrue();
		assertThat(root.get("productoId").asLong()).isEqualTo(2001L);
		assertThat(root.get("precio").decimalValue()).isEqualByComparingTo("49990.50");
		assertThat(root.get("correlationId").asText()).isEqualTo("corr-2");

		ProductoEvent deserialized = objectMapper.readValue(json, ProductoEvent.class);
		assertThat(deserialized.getOccurredAt()).isEqualTo(event.getOccurredAt());
	}

}
