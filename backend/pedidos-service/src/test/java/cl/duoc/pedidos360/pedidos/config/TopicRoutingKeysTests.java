package cl.duoc.pedidos360.pedidos.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * EP3 (Fase 2): valida de forma determinista (sin broker) que las routing keys
 * canonicas derivadas con {@link RabbitMQConfig#topicRoutingKey(String)} quedan
 * cubiertas por el patron de binding del dominio y NO por el de otros dominios.
 *
 * Se implementa un matcher de topic exchange de RabbitMQ con la semantica de
 * comodines: '*' = exactamente una palabra, '#' = cero o mas palabras.
 */
class TopicRoutingKeysTests {

	@Test
	@DisplayName("topicRoutingKey compone prefijo de dominio + eventType")
	void shouldComposeRoutingKeyFromPrefixAndEventType() {
		assertThat(RabbitMQConfig.topicRoutingKey(RabbitMQConfig.PEDIDO_CREADO))
				.isEqualTo("pedidos.pedido.creado");

		assertThat(RabbitMQConfig.topicRoutingKey(RabbitMQConfig.PEDIDO_ACTUALIZADO))
				.isEqualTo("pedidos.pedido.actualizado");

		assertThat(RabbitMQConfig.topicRoutingKey(RabbitMQConfig.PEDIDO_CANCELADO))
				.isEqualTo("pedidos.pedido.cancelado");
	}

	@Test
	@DisplayName("Routing keys de pedidos validas son capturadas por el binding del dominio")
	void shouldMatchValidPedidoRoutingKeys() {
		for (String eventType : new String[] {
				RabbitMQConfig.PEDIDO_CREADO,
				RabbitMQConfig.PEDIDO_ACTUALIZADO,
				RabbitMQConfig.PEDIDO_CANCELADO
		}) {
			String routingKey = RabbitMQConfig.topicRoutingKey(eventType);
			assertThat(matches(RabbitMQConfig.TOPIC_BINDING_PEDIDOS, routingKey))
					.as("binding %s debe capturar %s", RabbitMQConfig.TOPIC_BINDING_PEDIDOS, routingKey)
					.isTrue();
			assertThat(matches(RabbitMQConfig.TOPIC_BINDING_ALL, routingKey))
					.as("binding de auditoria %s debe capturar %s", RabbitMQConfig.TOPIC_BINDING_ALL, routingKey)
					.isTrue();
		}
	}

	@Test
	@DisplayName("Routing keys de otros dominios NO son capturadas por el binding de pedidos")
	void shouldNotMatchForeignRoutingKeys() {
		assertThat(matches(RabbitMQConfig.TOPIC_BINDING_PEDIDOS, "productos.producto.creado")).isFalse();
		assertThat(matches(RabbitMQConfig.TOPIC_BINDING_PEDIDOS, "productos.producto.stock.actualizado")).isFalse();
		// Subdominio hipotetico no previsto: el patron especifico lo descarta.
		assertThat(matches(RabbitMQConfig.TOPIC_BINDING_PEDIDOS, "pedidos.stock.actualizado")).isFalse();
	}

	@Test
	@DisplayName("El patron de auditoria captura eventos de cualquier dominio")
	void shouldMatchAllDomainsForAuditoria() {
		assertThat(matches(RabbitMQConfig.TOPIC_BINDING_ALL, "pedidos.pedido.creado")).isTrue();
		assertThat(matches(RabbitMQConfig.TOPIC_BINDING_ALL, "productos.producto.creado")).isTrue();
		assertThat(matches(RabbitMQConfig.TOPIC_BINDING_ALL, "otro.dominio.evento")).isTrue();
	}

	/**
	 * Matcher de topic exchange RabbitMQ: '*' = una palabra, '#' = cero o mas.
	 */
	private static boolean matches(String pattern, String routingKey) {
		String[] p = pattern.split("\\.");
		String[] k = routingKey.split("\\.");
		return matchFrom(0, 0, p, k);
	}

	private static boolean matchFrom(int pi, int ki, String[] p, String[] k) {
		// # consume cero o mas palabras (incluye resto completo).
		while (pi < p.length && "#".equals(p[pi])) {
			if (pi == p.length - 1) {
				return true;
			}
			for (int skip = ki; skip <= k.length; skip++) {
				if (matchFrom(pi + 1, skip, p, k)) {
					return true;
				}
			}
			return false;
		}
		if (pi == p.length && ki == k.length) {
			return true;
		}
		if (pi == p.length || ki == k.length) {
			return false;
		}
		if (!"*".equals(p[pi]) && !p[pi].equals(k[ki])) {
			return false;
		}
		return matchFrom(pi + 1, ki + 1, p, k);
	}

}
