package cl.duoc.pedidos360.pedidos.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cl.duoc.pedidos360.pedidos.dto.PedidoRequest;
import cl.duoc.pedidos360.pedidos.dto.PedidoResponse;
import cl.duoc.pedidos360.pedidos.entity.EstadoPedido;
import cl.duoc.pedidos360.pedidos.entity.Pedido;
import cl.duoc.pedidos360.pedidos.exception.ResourceNotFoundException;
import cl.duoc.pedidos360.pedidos.repository.PedidoRepository;

@Service
@Transactional
public class PedidoServiceImpl implements PedidoService {

	private final PedidoRepository pedidoRepository;

	public PedidoServiceImpl(PedidoRepository pedidoRepository) {
		this.pedidoRepository = pedidoRepository;
	}

	@Override
	public PedidoResponse crear(PedidoRequest request) {
		Pedido pedido = new Pedido(
				null,
				request.getUsuario().trim(),
				LocalDateTime.now(),
				request.getEstado() != null ? request.getEstado() : EstadoPedido.PENDIENTE,
				request.getTotal()
		);
		Pedido guardado = pedidoRepository.save(pedido);
		return PedidoResponse.fromEntity(guardado);
	}

	@Override
	@Transactional(readOnly = true)
	public List<PedidoResponse> listar() {
		return pedidoRepository.findAll()
				.stream()
				.map(PedidoResponse::fromEntity)
				.collect(Collectors.toList());
	}

	@Override
	@Transactional(readOnly = true)
	public List<PedidoResponse> listarPorUsuario(String usuario) {
		return pedidoRepository.findByUsuario(usuario.trim())
				.stream()
				.map(PedidoResponse::fromEntity)
				.collect(Collectors.toList());
	}

	@Override
	@Transactional(readOnly = true)
	public PedidoResponse buscarPorId(Long id) {
		Pedido pedido = pedidoRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Pedido con ID " + id + " no encontrado"));
		return PedidoResponse.fromEntity(pedido);
	}

	@Override
	public PedidoResponse actualizar(Long id, PedidoRequest request) {
		Pedido pedido = pedidoRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Pedido con ID " + id + " no encontrado"));

		pedido.setUsuario(request.getUsuario().trim());
		pedido.setTotal(request.getTotal());
		if (request.getEstado() != null) {
			pedido.setEstado(request.getEstado());
		}

		Pedido actualizado = pedidoRepository.save(pedido);
		return PedidoResponse.fromEntity(actualizado);
	}

	@Override
	public void cancelar(Long id) {
		Pedido pedido = pedidoRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Pedido con ID " + id + " no encontrado"));
		pedido.setEstado(EstadoPedido.CANCELADO);
		pedidoRepository.save(pedido);
	}

	@Override
	public void eliminar(Long id) {
		if (!pedidoRepository.existsById(id)) {
			throw new ResourceNotFoundException("Pedido con ID " + id + " no encontrado");
		}
		pedidoRepository.deleteById(id);
	}

}
