package cl.duoc.pedidos360.bff.exception;

public class DownstreamServiceUnavailableException extends RuntimeException {

	public DownstreamServiceUnavailableException(String serviceName, Throwable cause) {
		super("El servicio downstream (" + serviceName + ") no esta disponible actualmente", cause);
	}

}
