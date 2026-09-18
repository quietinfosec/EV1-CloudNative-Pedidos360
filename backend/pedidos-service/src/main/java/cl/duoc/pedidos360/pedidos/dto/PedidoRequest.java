package cl.duoc.pedidos360.pedidos.dto;

import java.math.BigDecimal;

import cl.duoc.pedidos360.pedidos.entity.EstadoPedido;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class PedidoRequest {

	@NotBlank(message = "El usuario es obligatorio")
	@Size(max = 150, message = "El usuario no puede exceder 150 caracteres")
	private String usuario;

	@NotNull(message = "El total es obligatorio")
	@DecimalMin(value = "0.0", inclusive = true, message = "El total no puede ser negativo")
	@Digits(integer = 10, fraction = 2, message = "Formato de total invalido")
	private BigDecimal total;

	private EstadoPedido estado = EstadoPedido.PENDIENTE;

	public PedidoRequest() {
	}

	public PedidoRequest(String usuario, BigDecimal total, EstadoPedido estado) {
		this.usuario = usuario;
		this.total = total;
		this.estado = estado != null ? estado : EstadoPedido.PENDIENTE;
	}

	public String getUsuario() {
		return usuario;
	}

	public void setUsuario(String usuario) {
		this.usuario = usuario;
	}

	public BigDecimal getTotal() {
		return total;
	}

	public void setTotal(BigDecimal total) {
		this.total = total;
	}

	public EstadoPedido getEstado() {
		return estado;
	}

	public void setEstado(EstadoPedido estado) {
		this.estado = estado;
	}

}
