package cl.duoc.pedidos360.rabbitmqadmin.exception;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.validation.ConstraintViolationException;

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

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<Map<String, Object>> handleBodyValidation(MethodArgumentNotValidException ex) {
		Map<String, String> fields = ex.getBindingResult().getFieldErrors().stream()
				.collect(Collectors.toMap(
						error -> error.getField(),
						error -> error.getDefaultMessage() == null ? "valor invalido" : error.getDefaultMessage(),
						(first, second) -> first,
						LinkedHashMap::new
				));
		return ResponseEntity.badRequest()
				.body(Map.of("error", "validacion_invalida", "fields", fields));
	}

	@ExceptionHandler(ConstraintViolationException.class)
	public ResponseEntity<Map<String, Object>> handleParameterValidation(ConstraintViolationException ex) {
		Map<String, String> fields = ex.getConstraintViolations().stream()
				.collect(Collectors.toMap(
						violation -> violation.getPropertyPath().toString(),
						violation -> violation.getMessage() == null ? "valor invalido" : violation.getMessage(),
						(first, second) -> first,
						LinkedHashMap::new
				));
		return ResponseEntity.badRequest()
				.body(Map.of("error", "validacion_invalida", "fields", fields));
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException ex) {
		return ResponseEntity.badRequest()
				.body(Map.of(
						"error", "solicitud_invalida",
						"message", ex.getMessage() == null ? "Solicitud invalida" : ex.getMessage()
				));
	}

}
