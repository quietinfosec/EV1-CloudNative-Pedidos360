package cl.duoc.pedidos360.rabbitmqadmin.dto;

import java.util.Map;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * EP3: datos para crear un binding cola <-> exchange via Management API.
 */
public record CreateBindingRequest(
		@NotBlank(message = "exchange es obligatorio")
		@Size(max = 255, message = "exchange no puede superar 255 caracteres")
		@Pattern(
				regexp = "^(?!amq\\.)\\S(?:.*\\S)?$",
				message = "exchange no puede empezar con amq. ni tener espacios al inicio/fin"
		)
		String exchange,

		@NotBlank(message = "queue es obligatorio")
		@Size(max = 255, message = "queue no puede superar 255 caracteres")
		@Pattern(
				regexp = "^(?!amq\\.)\\S(?:.*\\S)?$",
				message = "queue no puede empezar con amq. ni tener espacios al inicio/fin"
		)
		String queue,

		@Size(max = 255, message = "vhost no puede superar 255 caracteres")
		String vhost,

		@Size(max = 255, message = "routingKey no puede superar 255 caracteres")
		String routingKey,

		Map<String, Object> arguments
) {

	public CreateBindingRequest {
		vhost = (vhost == null || vhost.isBlank()) ? "/" : vhost;
		routingKey = routingKey == null ? "" : routingKey;
		arguments = arguments == null ? Map.of() : arguments;
	}

}
