package cl.duoc.pedidos360.bff.exception;

import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;

import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice
public class GlobalBffExceptionHandler {

	@ExceptionHandler(DownstreamServiceUnavailableException.class)
	public ResponseEntity<Map<String, Object>> handleDownstreamUnavailable(DownstreamServiceUnavailableException ex, HttpServletRequest request) {
		Map<String, Object> error = Map.of(
				"timestamp", LocalDateTime.now().toString(),
				"status", HttpStatus.SERVICE_UNAVAILABLE.value(),
				"error", HttpStatus.SERVICE_UNAVAILABLE.getReasonPhrase(),
				"message", ex.getMessage(),
				"path", request.getRequestURI()
		);
		return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(error);
	}

	@ExceptionHandler(ResourceAccessException.class)
	public ResponseEntity<Map<String, Object>> handleResourceAccess(ResourceAccessException ex, HttpServletRequest request) {
		Map<String, Object> error = Map.of(
				"timestamp", LocalDateTime.now().toString(),
				"status", HttpStatus.SERVICE_UNAVAILABLE.value(),
				"error", HttpStatus.SERVICE_UNAVAILABLE.getReasonPhrase(),
				"message", "Fallo de conexion hacia el microservicio interno",
				"path", request.getRequestURI()
		);
		return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(error);
	}

	@ExceptionHandler(HttpStatusCodeException.class)
	public ResponseEntity<String> handleHttpStatusCodeException(HttpStatusCodeException ex) {
		return ResponseEntity.status(ex.getStatusCode())
				.contentType(ex.getResponseHeaders() != null && ex.getResponseHeaders().getContentType() != null
						? ex.getResponseHeaders().getContentType()
						: org.springframework.http.MediaType.APPLICATION_JSON)
				.body(ex.getResponseBodyAsString());
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<Map<String, Object>> handleGeneral(Exception ex, HttpServletRequest request) {
		Map<String, Object> error = Map.of(
				"timestamp", LocalDateTime.now().toString(),
				"status", HttpStatus.INTERNAL_SERVER_ERROR.value(),
				"error", HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
				"message", "Error interno en el BFF",
				"path", request.getRequestURI()
		);
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
	}

}
