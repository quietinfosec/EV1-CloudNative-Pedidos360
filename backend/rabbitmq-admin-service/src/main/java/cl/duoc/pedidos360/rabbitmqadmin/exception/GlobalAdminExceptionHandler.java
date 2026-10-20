package cl.duoc.pedidos360.rabbitmqadmin.exception;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalAdminExceptionHandler {

	@ExceptionHandler(RabbitManagementException.class)
	public ResponseEntity<Map<String, String>> handleRabbitManagement(RabbitManagementException ex) {
		return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
				.body(Map.of(
						"error", "rabbitmq_management_unavailable",
						"message", ex.getMessage() == null ? "Error consultando RabbitMQ" : ex.getMessage()
				));
	}

}
