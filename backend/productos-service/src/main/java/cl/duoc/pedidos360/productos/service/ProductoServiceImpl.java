package cl.duoc.pedidos360.productos.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cl.duoc.pedidos360.productos.dto.ProductoRequest;
import cl.duoc.pedidos360.productos.dto.ProductoResponse;
import cl.duoc.pedidos360.productos.entity.Producto;
import cl.duoc.pedidos360.productos.exception.ResourceNotFoundException;
import cl.duoc.pedidos360.productos.repository.ProductoRepository;

@Service
@Transactional
public class ProductoServiceImpl implements ProductoService {

	private final ProductoRepository productoRepository;

	public ProductoServiceImpl(ProductoRepository productoRepository) {
		this.productoRepository = productoRepository;
	}

	@Override
	public ProductoResponse crear(ProductoRequest request) {
		Producto producto = new Producto(
				null,
				request.getNombre().trim(),
				request.getDescripcion(),
				request.getPrecio(),
				request.getStock(),
				true
		);
		Producto guardado = productoRepository.save(producto);
		return ProductoResponse.fromEntity(guardado);
	}

	@Override
	@Transactional(readOnly = true)
	public List<ProductoResponse> listar() {
		return productoRepository.findByActivoTrue()
				.stream()
				.map(ProductoResponse::fromEntity)
				.collect(Collectors.toList());
	}

	@Override
	@Transactional(readOnly = true)
	public List<ProductoResponse> listarTodos() {
		return productoRepository.findAll()
				.stream()
				.map(ProductoResponse::fromEntity)
				.collect(Collectors.toList());
	}

	@Override
	@Transactional(readOnly = true)
	public ProductoResponse buscarPorId(Long id) {
		Producto producto = productoRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Producto con ID " + id + " no encontrado"));
		return ProductoResponse.fromEntity(producto);
	}

	@Override
	public ProductoResponse actualizar(Long id, ProductoRequest request) {
		Producto producto = productoRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Producto con ID " + id + " no encontrado"));

		producto.setNombre(request.getNombre().trim());
		producto.setDescripcion(request.getDescripcion());
		producto.setPrecio(request.getPrecio());
		producto.setStock(request.getStock());

		Producto actualizado = productoRepository.save(producto);
		return ProductoResponse.fromEntity(actualizado);
	}

	@Override
	public void eliminar(Long id) {
		Producto producto = productoRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Producto con ID " + id + " no encontrado"));
		producto.setActivo(false);
		productoRepository.save(producto);
	}

}
