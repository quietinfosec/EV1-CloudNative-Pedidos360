package cl.duoc.pedidos360.rabbitmqadmin.dto;

import java.util.Map;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * EP3: datos para crear un exchange via Management API.
 */
public record CreateExchangeRequest(
		@NotBlank(message = "name es obligatorio")
		@Size(max = 255, message = "name no puede superar 255 caracteres")
		@Pattern(
				regexp = "^(?!amq\\.)\\S(?:.*\\S)?$",
				message = "name no puede empezar con amq. ni tener espacios al inicio/fin"
		)
		String name,

		@Size(max = 255, message = "vhost no puede superar 255 caracteres")
		String vhost,

		@NotBlank(message = "type es obligatorio")
		@Pattern(regexp = "direct|topic|fanout|headers", message = "type debe ser direct, topic, fanout o headers")
		String type,

		Boolean durable,

		Boolean autoDelete,

		Boolean internal,

		Map<String, Object> arguments
) {

	public CreateExchangeRequest {
		vhost = (vhost == null || vhost.isBlank()) ? "/" : vhost;
		durable = durable == null ? Boolean.TRUE : durable;
		autoDelete = autoDelete == null ? Boolean.FALSE : autoDelete;
		internal = internal == null ? Boolean.FALSE : internal;
		arguments = arguments == null ? Map.of() : arguments;
	}

}
