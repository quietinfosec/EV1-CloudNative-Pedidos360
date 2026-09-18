package cl.duoc.pedidos360.pedidos.service;

import java.util.List;

import cl.duoc.pedidos360.pedidos.dto.PedidoRequest;
import cl.duoc.pedidos360.pedidos.dto.PedidoResponse;

public interface PedidoService {

	PedidoResponse crear(PedidoRequest request);

	List<PedidoResponse> listar();

	List<PedidoResponse> listarPorUsuario(String usuario);

	PedidoResponse buscarPorId(Long id);

	PedidoResponse actualizar(Long id, PedidoRequest request);

	void cancelar(Long id);

	void eliminar(Long id);

}
