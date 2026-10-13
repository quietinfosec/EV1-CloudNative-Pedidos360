package cl.duoc.pedidos360.pedidos.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cl.duoc.pedidos360.pedidos.config.RabbitMQConfig;
import cl.duoc.pedidos360.pedidos.dto.PedidoRequest;
import cl.duoc.pedidos360.pedidos.dto.PedidoResponse;
import cl.duoc.pedidos360.pedidos.entity.EstadoPedido;
import cl.duoc.pedidos360.pedidos.entity.Pedido;
import cl.duoc.pedidos360.pedidos.event.PedidoDomainEvent;
import cl.duoc.pedidos360.pedidos.exception.ResourceNotFoundException;
import cl.duoc.pedidos360.pedidos.repository.PedidoRepository;

@Service
@Transactional
public class PedidoServiceImpl implements PedidoService {

	private static final Logger log = LoggerFactory.getLogger(PedidoServiceImpl.class);

	private final PedidoRepository pedidoRepository;
	private final ApplicationEventPublisher applicationEventPublisher;

	public PedidoServiceImpl(PedidoRepository pedidoRepository, ApplicationEventPublisher applicationEventPublisher) {
		this.pedidoRepository = pedidoRepository;
		this.applicationEventPublisher = applicationEventPublisher;
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
		registerEvent(guardado, RabbitMQConfig.PEDIDO_CREADO);
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
		registerEvent(actualizado, RabbitMQConfig.PEDIDO_ACTUALIZADO);
		return PedidoResponse.fromEntity(actualizado);
	}

	@Override
	public void cancelar(Long id) {
		Pedido pedido = pedidoRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Pedido con ID " + id + " no encontrado"));
		pedido.setEstado(EstadoPedido.CANCELADO);
		Pedido cancelado = pedidoRepository.save(pedido);
		registerEvent(cancelado, RabbitMQConfig.PEDIDO_CANCELADO);
	}

	@Override
	public void eliminar(Long id) {
		if (!pedidoRepository.existsById(id)) {
			throw new ResourceNotFoundException("Pedido con ID " + id + " no encontrado");
		}
		pedidoRepository.deleteById(id);
	}

	private void registerEvent(Pedido pedido, String eventType) {
		applicationEventPublisher.publishEvent(new PedidoDomainEvent(
				eventType,
				pedido.getId(),
				pedido.getEstado().name(),
				pedido.getUsuario(),
				pedido.getTotal(),
				null
		));
		log.debug("Evento interno registrado eventType={} pedidoId={}", eventType, pedido.getId());
	}

}
