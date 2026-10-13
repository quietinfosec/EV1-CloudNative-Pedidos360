package cl.duoc.pedidos360.pedidos.event;

import java.math.BigDecimal;

public class PedidoDomainEvent {

	private final String eventType;
	private final Long pedidoId;
	private final String estado;
	private final String usuarioId;
	private final BigDecimal total;
	private final String correlationId;

	public PedidoDomainEvent(String eventType, Long pedidoId, String estado, String usuarioId, BigDecimal total, String correlationId) {
		this.eventType = eventType;
		this.pedidoId = pedidoId;
		this.estado = estado;
		this.usuarioId = usuarioId;
		this.total = total;
		this.correlationId = correlationId;
	}

	public String getEventType() {
		return eventType;
	}

	public Long getPedidoId() {
		return pedidoId;
	}

	public String getEstado() {
		return estado;
	}

	public String getUsuarioId() {
		return usuarioId;
	}

	public BigDecimal getTotal() {
		return total;
	}

	public String getCorrelationId() {
		return correlationId;
	}

}
