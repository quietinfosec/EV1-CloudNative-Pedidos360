package cl.duoc.pedidos360.productos.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import cl.duoc.pedidos360.productos.entity.Producto;

public class ProductoResponse {

	private Long id;
	private String nombre;
	private String descripcion;
	private BigDecimal precio;
	private Integer stock;
	private Boolean activo;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;

	public ProductoResponse() {
	}

	public ProductoResponse(Long id, String nombre, String descripcion, BigDecimal precio, Integer stock, Boolean activo, LocalDateTime createdAt, LocalDateTime updatedAt) {
		this.id = id;
		this.nombre = nombre;
		this.descripcion = descripcion;
		this.precio = precio;
		this.stock = stock;
		this.activo = activo;
		this.createdAt = createdAt;
		this.updatedAt = updatedAt;
	}

	public static ProductoResponse fromEntity(Producto entity) {
		if (entity == null) {
			return null;
		}
		return new ProductoResponse(
				entity.getId(),
				entity.getNombre(),
				entity.getDescripcion(),
				entity.getPrecio(),
				entity.getStock(),
				entity.getActivo(),
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

	public String getNombre() {
		return nombre;
	}

	public void setNombre(String nombre) {
		this.nombre = nombre;
	}

	public String getDescripcion() {
		return descripcion;
	}

	public void setDescripcion(String descripcion) {
		this.descripcion = descripcion;
	}

	public BigDecimal getPrecio() {
		return precio;
	}

	public void setPrecio(BigDecimal precio) {
		this.precio = precio;
	}

	public Integer getStock() {
		return stock;
	}

	public void setStock(Integer stock) {
		this.stock = stock;
	}

	public Boolean getActivo() {
		return activo;
	}

	public void setActivo(Boolean activo) {
		this.activo = activo;
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
