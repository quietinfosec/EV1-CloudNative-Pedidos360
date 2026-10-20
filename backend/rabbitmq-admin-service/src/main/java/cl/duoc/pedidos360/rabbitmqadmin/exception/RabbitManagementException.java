package cl.duoc.pedidos360.rabbitmqadmin.exception;

/**
 * Error de comunicacion o respuesta invalida desde la Management API de RabbitMQ.
 */
public class RabbitManagementException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public RabbitManagementException(String message, Throwable cause) {
		super(message, cause);
	}

	public RabbitManagementException(String message) {
		super(message);
	}

}
