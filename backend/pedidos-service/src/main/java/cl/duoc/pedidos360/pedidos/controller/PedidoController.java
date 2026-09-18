package cl.duoc.pedidos360.pedidos.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
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

import cl.duoc.pedidos360.pedidos.dto.PedidoRequest;
import cl.duoc.pedidos360.pedidos.dto.PedidoResponse;
import cl.duoc.pedidos360.pedidos.service.PedidoService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/pedidos")
public class PedidoController {

	private final PedidoService pedidoService;

	public PedidoController(PedidoService pedidoService) {
		this.pedidoService = pedidoService;
	}

	@PostMapping
	public ResponseEntity<PedidoResponse> crear(@Valid @RequestBody PedidoRequest request) {
		PedidoResponse creado = pedidoService.crear(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(creado);
	}

	@GetMapping
	public ResponseEntity<List<PedidoResponse>> listar(@RequestParam(required = false) String usuario) {
		List<PedidoResponse> pedidos = (usuario != null && !usuario.isBlank())
				? pedidoService.listarPorUsuario(usuario)
				: pedidoService.listar();
		return ResponseEntity.ok(pedidos);
	}

	@GetMapping("/{id}")
	public ResponseEntity<PedidoResponse> buscarPorId(@PathVariable Long id) {
		PedidoResponse pedido = pedidoService.buscarPorId(id);
		return ResponseEntity.ok(pedido);
	}

	@GetMapping("/usuario/{usuario}")
	public ResponseEntity<List<PedidoResponse>> buscarPorUsuario(@PathVariable String usuario) {
		List<PedidoResponse> pedidos = pedidoService.listarPorUsuario(usuario);
		return ResponseEntity.ok(pedidos);
	}

	@PutMapping("/{id}")
	public ResponseEntity<PedidoResponse> actualizar(@PathVariable Long id, @Valid @RequestBody PedidoRequest request) {
		PedidoResponse actualizado = pedidoService.actualizar(id, request);
		return ResponseEntity.ok(actualizado);
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> eliminar(@PathVariable Long id) {
		pedidoService.eliminar(id);
		return ResponseEntity.noContent().build();
	}

}
