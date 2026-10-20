package cl.duoc.pedidos360.rabbitmqadmin.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Resumen de un exchange expuesto por la Management API de RabbitMQ.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ExchangeSummary(
		String name,
		String type,
		boolean durable
) {
}
