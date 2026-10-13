package cl.duoc.pedidos360.productos.event;

import java.math.BigDecimal;

public class ProductoDomainEvent {

	private final String eventType;
	private final Long productoId;
	private final String nombre;
	private final Integer stock;
	private final BigDecimal precio;
	private final String correlationId;

	public ProductoDomainEvent(String eventType, Long productoId, String nombre, Integer stock, BigDecimal precio, String correlationId) {
		this.eventType = eventType;
		this.productoId = productoId;
		this.nombre = nombre;
		this.stock = stock;
		this.precio = precio;
		this.correlationId = correlationId;
	}

	public String getEventType() {
		return eventType;
	}

	public Long getProductoId() {
		return productoId;
	}

	public String getNombre() {
		return nombre;
	}

	public Integer getStock() {
		return stock;
	}

	public BigDecimal getPrecio() {
		return precio;
	}

	public String getCorrelationId() {
		return correlationId;
	}

}
