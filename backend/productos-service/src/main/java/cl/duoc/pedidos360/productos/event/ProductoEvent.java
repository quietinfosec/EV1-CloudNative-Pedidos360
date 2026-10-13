package cl.duoc.pedidos360.productos.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class ProductoEvent {

	private UUID eventId;
	private String eventType;
	private Integer eventVersion;
	private LocalDateTime occurredAt;
	private Long productoId;
	private String nombre;
	private Integer stock;
	private BigDecimal precio;
	private String correlationId;

	public ProductoEvent() {
		this.eventId = UUID.randomUUID();
		this.eventVersion = 1;
		this.occurredAt = LocalDateTime.now();
	}

	public ProductoEvent(String eventType, Long productoId, String nombre, Integer stock, BigDecimal precio, String correlationId) {
		this();
		this.eventType = eventType;
		this.productoId = productoId;
		this.nombre = nombre;
		this.stock = stock;
		this.precio = precio;
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

	public Long getProductoId() {
		return productoId;
	}

	public void setProductoId(Long productoId) {
		this.productoId = productoId;
	}

	public String getNombre() {
		return nombre;
	}

	public void setNombre(String nombre) {
		this.nombre = nombre;
	}

	public Integer getStock() {
		return stock;
	}

	public void setStock(Integer stock) {
		this.stock = stock;
	}

	public BigDecimal getPrecio() {
		return precio;
	}

	public void setPrecio(BigDecimal precio) {
		this.precio = precio;
	}

	public String getCorrelationId() {
		return correlationId;
	}

	public void setCorrelationId(String correlationId) {
		this.correlationId = correlationId;
	}

}
