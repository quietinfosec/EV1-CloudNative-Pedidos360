package cl.duoc.pedidos360.rabbitmqadmin.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Resumen de una cola expuesto por la Management API de RabbitMQ.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record QueueSummary(
		String name,
		boolean durable,
		int messages,
		int consumers,
		String state
) {
}
