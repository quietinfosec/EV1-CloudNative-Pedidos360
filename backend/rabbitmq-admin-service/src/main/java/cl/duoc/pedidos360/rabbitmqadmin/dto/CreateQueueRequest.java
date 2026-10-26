package cl.duoc.pedidos360.rabbitmqadmin.dto;

import java.util.Map;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * EP3: datos para crear una cola via Management API.
 */
public record CreateQueueRequest(
		@NotBlank(message = "name es obligatorio")
		@Size(max = 255, message = "name no puede superar 255 caracteres")
		@Pattern(
				regexp = "^(?!amq\\.)\\S(?:.*\\S)?$",
				message = "name no puede empezar con amq. ni tener espacios al inicio/fin"
		)
		String name,

		@Size(max = 255, message = "vhost no puede superar 255 caracteres")
		String vhost,

		Boolean durable,

		Boolean autoDelete,

		Map<String, Object> arguments
) {

	public CreateQueueRequest {
		vhost = (vhost == null || vhost.isBlank()) ? "/" : vhost;
		durable = durable == null ? Boolean.TRUE : durable;
		autoDelete = autoDelete == null ? Boolean.FALSE : autoDelete;
		arguments = arguments == null ? Map.of() : arguments;
	}

}
