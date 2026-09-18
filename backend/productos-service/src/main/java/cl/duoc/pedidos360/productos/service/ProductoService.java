package cl.duoc.pedidos360.productos.service;

import java.util.List;

import cl.duoc.pedidos360.productos.dto.ProductoRequest;
import cl.duoc.pedidos360.productos.dto.ProductoResponse;

public interface ProductoService {

	ProductoResponse crear(ProductoRequest request);

	List<ProductoResponse> listar();

	List<ProductoResponse> listarTodos();

	ProductoResponse buscarPorId(Long id);

	ProductoResponse actualizar(Long id, ProductoRequest request);

	void eliminar(Long id);

}
