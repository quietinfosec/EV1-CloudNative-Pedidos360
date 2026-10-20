package cl.duoc.pedidos360.productos.config;

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
		assertThat(RabbitMQConfig.topicRoutingKey(RabbitMQConfig.PRODUCTO_CREADO))
				.isEqualTo("productos.producto.creado");

		assertThat(RabbitMQConfig.topicRoutingKey(RabbitMQConfig.PRODUCTO_ACTUALIZADO))
				.isEqualTo("productos.producto.actualizado");

		assertThat(RabbitMQConfig.topicRoutingKey(RabbitMQConfig.PRODUCTO_STOCK_ACTUALIZADO))
				.isEqualTo("productos.producto.stock.actualizado");
	}

	@Test
	@DisplayName("Routing keys de productos validas son capturadas por el binding del dominio")
	void shouldMatchValidProductoRoutingKeys() {
		for (String eventType : new String[] {
				RabbitMQConfig.PRODUCTO_CREADO,
				RabbitMQConfig.PRODUCTO_ACTUALIZADO,
				RabbitMQConfig.PRODUCTO_STOCK_ACTUALIZADO
		}) {
			String routingKey = RabbitMQConfig.topicRoutingKey(eventType);
			assertThat(matches(RabbitMQConfig.TOPIC_BINDING_PRODUCTOS, routingKey))
					.as("binding %s debe capturar %s", RabbitMQConfig.TOPIC_BINDING_PRODUCTOS, routingKey)
					.isTrue();
			assertThat(matches(RabbitMQConfig.TOPIC_BINDING_ALL, routingKey))
					.as("binding de auditoria %s debe capturar %s", RabbitMQConfig.TOPIC_BINDING_ALL, routingKey)
					.isTrue();
		}
	}

	@Test
	@DisplayName("Routing keys de otros dominios NO son capturadas por el binding de productos")
	void shouldNotMatchForeignRoutingKeys() {
		assertThat(matches(RabbitMQConfig.TOPIC_BINDING_PRODUCTOS, "pedidos.pedido.creado")).isFalse();
		assertThat(matches(RabbitMQConfig.TOPIC_BINDING_PRODUCTOS, "pedidos.pedido.cancelado")).isFalse();
		// Subdominio hipotetico no previsto: el patron especifico lo descarta.
		assertThat(matches(RabbitMQConfig.TOPIC_BINDING_PRODUCTOS, "productos.pedido.creado")).isFalse();
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
