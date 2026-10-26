package cl.duoc.pedidos360.rabbitmqadmin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * EP3: publica un mensaje de prueba desde el panel de administracion.
 * Permite demostrar en vivo el flujo ACK/NACK/DLQ sin usar la UI de RabbitMQ.
 */
public record PublishMessageRequest(
		@NotBlank(message = "exchange es obligatorio")
		@Size(max = 255, message = "exchange no puede superar 255 caracteres")
		@Pattern(
				regexp = "^(?!amq\\.)\\S(?:.*\\S)?$",
				message = "exchange no puede empezar con amq. ni tener espacios al inicio/fin"
		)
		String exchange,

		@Size(max = 255, message = "routingKey no puede superar 255 caracteres")
		String routingKey,

		@NotBlank(message = "payload es obligatorio")
		@Size(max = 1048576, message = "payload no puede superar 1 MB")
		String payload,

		@Size(max = 255, message = "vhost no puede superar 255 caracteres")
		String vhost
) {

	public PublishMessageRequest {
		routingKey = routingKey == null ? "" : routingKey;
		vhost = (vhost == null || vhost.isBlank()) ? "/" : vhost;
	}

}
