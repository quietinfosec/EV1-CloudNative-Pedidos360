package cl.duoc.pedidos360.pedidos.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class PedidoEvent {

	private UUID eventId;
	private String eventType;
	private Integer eventVersion;
	private LocalDateTime occurredAt;
	private Long pedidoId;
	private String estado;
	private String usuarioId;
	private BigDecimal total;
	private String correlationId;

	public PedidoEvent() {
		this.eventId = UUID.randomUUID();
		this.eventVersion = 1;
		this.occurredAt = LocalDateTime.now();
	}

	public PedidoEvent(String eventType, Long pedidoId, String estado, String usuarioId, BigDecimal total, String correlationId) {
		this();
		this.eventType = eventType;
		this.pedidoId = pedidoId;
		this.estado = estado;
		this.usuarioId = usuarioId;
		this.total = total;
		this.correlationId = correlationId;
	}

	public UUID getEventId() {
		return eventId;
	}

	public void setEventId(UUID eventId) {
		this.eventId = eventId;
	}

	public String getEventType() {
		return eventType;
	}

	public void setEventType(String eventType) {
		this.eventType = eventType;
	}

	public Integer getEventVersion() {
		return eventVersion;
	}

	public void setEventVersion(Integer eventVersion) {
		this.eventVersion = eventVersion;
	}

	public LocalDateTime getOccurredAt() {
		return occurredAt;
	}

	public void setOccurredAt(LocalDateTime occurredAt) {
		this.occurredAt = occurredAt;
	}

	public Long getPedidoId() {
		return pedidoId;
	}

	public void setPedidoId(Long pedidoId) {
		this.pedidoId = pedidoId;
	}

	public String getEstado() {
		return estado;
	}

	public void setEstado(String estado) {
		this.estado = estado;
	}

	public String getUsuarioId() {
		return usuarioId;
	}

	public void setUsuarioId(String usuarioId) {
		this.usuarioId = usuarioId;
	}

	public BigDecimal getTotal() {
		return total;
	}

	public void setTotal(BigDecimal total) {
		this.total = total;
	}

	public String getCorrelationId() {
		return correlationId;
	}

	public void setCorrelationId(String correlationId) {
		this.correlationId = correlationId;
	}

}
