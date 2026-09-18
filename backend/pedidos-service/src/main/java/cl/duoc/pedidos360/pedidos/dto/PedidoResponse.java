package cl.duoc.pedidos360.pedidos.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import cl.duoc.pedidos360.pedidos.entity.EstadoPedido;
import cl.duoc.pedidos360.pedidos.entity.Pedido;

public class PedidoResponse {

	private Long id;
	private String usuario;
	private LocalDateTime fecha;
	private EstadoPedido estado;
	private BigDecimal total;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;

	public PedidoResponse() {
	}

	public PedidoResponse(Long id, String usuario, LocalDateTime fecha, EstadoPedido estado, BigDecimal total, LocalDateTime createdAt, LocalDateTime updatedAt) {
		this.id = id;
		this.usuario = usuario;
		this.fecha = fecha;
		this.estado = estado;
		this.total = total;
		this.createdAt = createdAt;
		this.updatedAt = updatedAt;
	}

	public static PedidoResponse fromEntity(Pedido entity) {
		if (entity == null) {
			return null;
		}
		return new PedidoResponse(
				entity.getId(),
				entity.getUsuario(),
				entity.getFecha(),
				entity.getEstado(),
				entity.getTotal(),
				entity.getCreatedAt(),
				entity.getUpdatedAt()
		);
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getUsuario() {
		return usuario;
	}

	public void setUsuario(String usuario) {
		this.usuario = usuario;
	}

	public LocalDateTime getFecha() {
		return fecha;
	}

	public void setFecha(LocalDateTime fecha) {
		this.fecha = fecha;
	}

	public EstadoPedido getEstado() {
		return estado;
	}

	public void setEstado(EstadoPedido estado) {
		this.estado = estado;
	}

	public BigDecimal getTotal() {
		return total;
	}

	public void setTotal(BigDecimal total) {
		this.total = total;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}

	public LocalDateTime getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(LocalDateTime updatedAt) {
		this.updatedAt = updatedAt;
	}

}
