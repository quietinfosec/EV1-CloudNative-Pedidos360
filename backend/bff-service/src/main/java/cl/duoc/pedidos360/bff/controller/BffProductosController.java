package cl.duoc.pedidos360.bff.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import cl.duoc.pedidos360.bff.client.ProductosClient;

@RestController
@RequestMapping("/api/productos")
public class BffProductosController {

	private final ProductosClient productosClient;

	public BffProductosController(ProductosClient productosClient) {
		this.productosClient = productosClient;
	}

	@GetMapping
	public ResponseEntity<String> getAll(@RequestParam(required = false, defaultValue = "true") boolean soloActivos) {
		ResponseEntity<String> response = productosClient.getAll(soloActivos);
		return ResponseEntity.status(response.getStatusCode())
				.contentType(MediaType.APPLICATION_JSON)
				.body(response.getBody());
	}

	@GetMapping("/{id}")
	public ResponseEntity<String> getById(@PathVariable Long id) {
		ResponseEntity<String> response = productosClient.getById(id);
		return ResponseEntity.status(response.getStatusCode())
				.contentType(MediaType.APPLICATION_JSON)
				.body(response.getBody());
	}

	@PostMapping
	public ResponseEntity<String> create(@RequestBody String requestBody) {
		ResponseEntity<String> response = productosClient.create(requestBody);
		return ResponseEntity.status(response.getStatusCode())
				.contentType(MediaType.APPLICATION_JSON)
				.body(response.getBody());
	}

	@PutMapping("/{id}")
	public ResponseEntity<String> update(@PathVariable Long id, @RequestBody String requestBody) {
		ResponseEntity<String> response = productosClient.update(id, requestBody);
		return ResponseEntity.status(response.getStatusCode())
				.contentType(MediaType.APPLICATION_JSON)
				.body(response.getBody());
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(@PathVariable Long id) {
		ResponseEntity<Void> response = productosClient.delete(id);
		return ResponseEntity.status(response.getStatusCode()).build();
	}

}
