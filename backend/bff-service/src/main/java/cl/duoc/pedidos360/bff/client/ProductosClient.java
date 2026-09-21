package cl.duoc.pedidos360.bff.client;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.ResourceAccessException;

import cl.duoc.pedidos360.bff.exception.DownstreamServiceUnavailableException;

@Component
public class ProductosClient {

	private final RestClient restClient;

	public ProductosClient(@Qualifier("productosRestClient") RestClient restClient) {
		this.restClient = restClient;
	}

	public ResponseEntity<String> getAll(boolean soloActivos) {
		try {
			return restClient.get()
					.uri(uriBuilder -> uriBuilder.path("/api/productos")
							.queryParam("soloActivos", soloActivos)
							.build())
					.accept(MediaType.APPLICATION_JSON)
					.retrieve()
					.toEntity(String.class);
		} catch (ResourceAccessException ex) {
			throw new DownstreamServiceUnavailableException("productos-service", ex);
		}
	}

	public ResponseEntity<String> getById(Long id) {
		try {
			return restClient.get()
					.uri("/api/productos/{id}", id)
					.accept(MediaType.APPLICATION_JSON)
					.retrieve()
					.toEntity(String.class);
		} catch (ResourceAccessException ex) {
			throw new DownstreamServiceUnavailableException("productos-service", ex);
		}
	}

	public ResponseEntity<String> create(Object requestBody) {
		try {
			return restClient.post()
					.uri("/api/productos")
					.contentType(MediaType.APPLICATION_JSON)
					.body(requestBody)
					.retrieve()
					.toEntity(String.class);
		} catch (ResourceAccessException ex) {
			throw new DownstreamServiceUnavailableException("productos-service", ex);
		}
	}

	public ResponseEntity<String> update(Long id, Object requestBody) {
		try {
			return restClient.put()
					.uri("/api/productos/{id}", id)
					.contentType(MediaType.APPLICATION_JSON)
					.body(requestBody)
					.retrieve()
					.toEntity(String.class);
		} catch (ResourceAccessException ex) {
			throw new DownstreamServiceUnavailableException("productos-service", ex);
		}
	}

	public ResponseEntity<Void> delete(Long id) {
		try {
			return restClient.delete()
					.uri("/api/productos/{id}", id)
					.retrieve()
					.toBodilessEntity();
		} catch (ResourceAccessException ex) {
			throw new DownstreamServiceUnavailableException("productos-service", ex);
		}
	}

}
