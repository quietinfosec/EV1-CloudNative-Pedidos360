package cl.duoc.pedidos360.productos.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class ProductoRequest {

	@NotBlank(message = "El nombre es obligatorio")
	@Size(max = 100, message = "El nombre no puede exceder 100 caracteres")
	private String nombre;

	@Size(max = 500, message = "La descripcion no puede exceder 500 caracteres")
	private String descripcion;

	@NotNull(message = "El precio es obligatorio")
	@DecimalMin(value = "0.0", inclusive = true, message = "El precio debe ser mayor o igual a 0")
	@Digits(integer = 10, fraction = 2, message = "Formato de precio invalido")
	private BigDecimal precio;

	@NotNull(message = "El stock es obligatorio")
	@Min(value = 0, message = "El stock debe ser mayor o igual a 0")
	private Integer stock;

	public ProductoRequest() {
	}

	public ProductoRequest(String nombre, String descripcion, BigDecimal precio, Integer stock) {
		this.nombre = nombre;
		this.descripcion = descripcion;
		this.precio = precio;
		this.stock = stock;
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

}
