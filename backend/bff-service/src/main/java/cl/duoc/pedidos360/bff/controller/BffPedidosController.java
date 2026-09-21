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

import cl.duoc.pedidos360.bff.client.PedidosClient;

@RestController
@RequestMapping("/api/pedidos")
public class BffPedidosController {

	private final PedidosClient pedidosClient;

	public BffPedidosController(PedidosClient pedidosClient) {
		this.pedidosClient = pedidosClient;
	}

	@GetMapping
	public ResponseEntity<String> getAll(@RequestParam(required = false) String usuario) {
		ResponseEntity<String> response = pedidosClient.getAll(usuario);
		return ResponseEntity.status(response.getStatusCode())
				.contentType(MediaType.APPLICATION_JSON)
				.body(response.getBody());
	}

	@GetMapping("/{id}")
	public ResponseEntity<String> getById(@PathVariable Long id) {
		ResponseEntity<String> response = pedidosClient.getById(id);
		return ResponseEntity.status(response.getStatusCode())
				.contentType(MediaType.APPLICATION_JSON)
				.body(response.getBody());
	}

	@GetMapping("/usuario/{usuario}")
	public ResponseEntity<String> getByUsuario(@PathVariable String usuario) {
		ResponseEntity<String> response = pedidosClient.getByUsuario(usuario);
		return ResponseEntity.status(response.getStatusCode())
				.contentType(MediaType.APPLICATION_JSON)
				.body(response.getBody());
	}

	@PostMapping
	public ResponseEntity<String> create(@RequestBody String requestBody) {
		ResponseEntity<String> response = pedidosClient.create(requestBody);
		return ResponseEntity.status(response.getStatusCode())
				.contentType(MediaType.APPLICATION_JSON)
				.body(response.getBody());
	}

	@PutMapping("/{id}")
	public ResponseEntity<String> update(@PathVariable Long id, @RequestBody String requestBody) {
		ResponseEntity<String> response = pedidosClient.update(id, requestBody);
		return ResponseEntity.status(response.getStatusCode())
				.contentType(MediaType.APPLICATION_JSON)
				.body(response.getBody());
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(@PathVariable Long id) {
		ResponseEntity<Void> response = pedidosClient.delete(id);
		return ResponseEntity.status(response.getStatusCode()).build();
	}

}
