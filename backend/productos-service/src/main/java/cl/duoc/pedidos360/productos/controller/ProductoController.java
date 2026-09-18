package cl.duoc.pedidos360.productos.controller;

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

import cl.duoc.pedidos360.productos.dto.ProductoRequest;
import cl.duoc.pedidos360.productos.dto.ProductoResponse;
import cl.duoc.pedidos360.productos.service.ProductoService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/productos")
public class ProductoController {

	private final ProductoService productoService;

	public ProductoController(ProductoService productoService) {
		this.productoService = productoService;
	}

	@PostMapping
	public ResponseEntity<ProductoResponse> crear(@Valid @RequestBody ProductoRequest request) {
		ProductoResponse creado = productoService.crear(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(creado);
	}

	@GetMapping
	public ResponseEntity<List<ProductoResponse>> listar(@RequestParam(required = false, defaultValue = "true") boolean soloActivos) {
		List<ProductoResponse> productos = soloActivos ? productoService.listar() : productoService.listarTodos();
		return ResponseEntity.ok(productos);
	}

	@GetMapping("/{id}")
	public ResponseEntity<ProductoResponse> buscarPorId(@PathVariable Long id) {
		ProductoResponse producto = productoService.buscarPorId(id);
		return ResponseEntity.ok(producto);
	}

	@PutMapping("/{id}")
	public ResponseEntity<ProductoResponse> actualizar(@PathVariable Long id, @Valid @RequestBody ProductoRequest request) {
		ProductoResponse actualizado = productoService.actualizar(id, request);
		return ResponseEntity.ok(actualizado);
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> eliminar(@PathVariable Long id) {
		productoService.eliminar(id);
		return ResponseEntity.noContent().build();
	}

}
