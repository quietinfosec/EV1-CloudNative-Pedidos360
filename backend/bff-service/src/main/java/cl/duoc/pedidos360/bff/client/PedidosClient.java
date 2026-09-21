package cl.duoc.pedidos360.bff.client;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.ResourceAccessException;

import cl.duoc.pedidos360.bff.exception.DownstreamServiceUnavailableException;

@Component
public class PedidosClient {

	private final RestClient restClient;

	public PedidosClient(@Qualifier("pedidosRestClient") RestClient restClient) {
		this.restClient = restClient;
	}

	public ResponseEntity<String> getAll(String usuario) {
		try {
			return restClient.get()
					.uri(uriBuilder -> {
						var builder = uriBuilder.path("/api/pedidos");
						if (usuario != null && !usuario.isBlank()) {
							builder.queryParam("usuario", usuario);
						}
						return builder.build();
					})
					.accept(MediaType.APPLICATION_JSON)
					.retrieve()
					.toEntity(String.class);
		} catch (ResourceAccessException ex) {
			throw new DownstreamServiceUnavailableException("pedidos-service", ex);
		}
	}

	public ResponseEntity<String> getById(Long id) {
		try {
			return restClient.get()
					.uri("/api/pedidos/{id}", id)
					.accept(MediaType.APPLICATION_JSON)
					.retrieve()
					.toEntity(String.class);
		} catch (ResourceAccessException ex) {
			throw new DownstreamServiceUnavailableException("pedidos-service", ex);
		}
	}

	public ResponseEntity<String> getByUsuario(String usuario) {
		try {
			return restClient.get()
					.uri("/api/pedidos/usuario/{usuario}", usuario)
					.accept(MediaType.APPLICATION_JSON)
					.retrieve()
					.toEntity(String.class);
		} catch (ResourceAccessException ex) {
			throw new DownstreamServiceUnavailableException("pedidos-service", ex);
		}
	}

	public ResponseEntity<String> create(Object requestBody) {
		try {
			return restClient.post()
					.uri("/api/pedidos")
					.contentType(MediaType.APPLICATION_JSON)
					.body(requestBody)
					.retrieve()
					.toEntity(String.class);
		} catch (ResourceAccessException ex) {
			throw new DownstreamServiceUnavailableException("pedidos-service", ex);
		}
	}

	public ResponseEntity<String> update(Long id, Object requestBody) {
		try {
			return restClient.put()
					.uri("/api/pedidos/{id}", id)
					.contentType(MediaType.APPLICATION_JSON)
					.body(requestBody)
					.retrieve()
					.toEntity(String.class);
		} catch (ResourceAccessException ex) {
			throw new DownstreamServiceUnavailableException("pedidos-service", ex);
		}
	}

	public ResponseEntity<Void> delete(Long id) {
		try {
			return restClient.delete()
					.uri("/api/pedidos/{id}", id)
					.retrieve()
					.toBodilessEntity();
		} catch (ResourceAccessException ex) {
			throw new DownstreamServiceUnavailableException("pedidos-service", ex);
		}
	}

}
