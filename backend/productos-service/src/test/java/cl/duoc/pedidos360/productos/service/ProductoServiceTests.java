package cl.duoc.pedidos360.productos.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.context.ApplicationEventPublisher;
import org.mockito.junit.jupiter.MockitoExtension;

import cl.duoc.pedidos360.productos.config.RabbitMQConfig;
import cl.duoc.pedidos360.productos.dto.ProductoRequest;
import cl.duoc.pedidos360.productos.dto.ProductoResponse;
import cl.duoc.pedidos360.productos.entity.Producto;
import cl.duoc.pedidos360.productos.event.ProductoDomainEvent;
import cl.duoc.pedidos360.productos.exception.ResourceNotFoundException;
import cl.duoc.pedidos360.productos.repository.ProductoRepository;

@ExtendWith(MockitoExtension.class)
class ProductoServiceTests {

	@Mock
	private ProductoRepository productoRepository;

	@Mock
	private ApplicationEventPublisher applicationEventPublisher;

	@InjectMocks
	private ProductoServiceImpl productoService;

	private Producto producto;
	private ProductoRequest request;

	@BeforeEach
	void setUp() {
		producto = new Producto(1L, "Teclado Mecanico", "Switch Blue", new BigDecimal("49.99"), 25, true);
		request = new ProductoRequest("Teclado Mecanico", "Switch Blue", new BigDecimal("49.99"), 25);
	}

	@Test
	void shouldCrearProducto() {
		when(productoRepository.save(any(Producto.class))).thenReturn(producto);

		ProductoResponse result = productoService.crear(request);

		assertThat(result).isNotNull();
		assertThat(result.getNombre()).isEqualTo("Teclado Mecanico");
		verify(productoRepository).save(any(Producto.class));

		ArgumentCaptor<ProductoDomainEvent> eventCaptor = ArgumentCaptor.forClass(ProductoDomainEvent.class);
		verify(applicationEventPublisher).publishEvent(eventCaptor.capture());
		assertThat(eventCaptor.getValue().getEventType()).isEqualTo(RabbitMQConfig.PRODUCTO_CREADO);
		assertThat(eventCaptor.getValue().getProductoId()).isEqualTo(1L);
	}

	@Test
	void shouldListarProductos() {
		when(productoRepository.findByActivoTrue()).thenReturn(List.of(producto));

		List<ProductoResponse> result = productoService.listar();

		assertThat(result).hasSize(1);
		assertThat(result.get(0).getNombre()).isEqualTo("Teclado Mecanico");
	}

	@Test
	void shouldBuscarPorId() {
		when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));

		ProductoResponse result = productoService.buscarPorId(1L);

		assertThat(result).isNotNull();
		assertThat(result.getId()).isEqualTo(1L);
	}

	@Test
	void shouldThrowNotFoundWhenBuscarIdInexistente() {
		when(productoRepository.findById(99L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> productoService.buscarPorId(99L))
				.isInstanceOf(ResourceNotFoundException.class);
	}

	@Test
	void shouldRegisterStockEventWhenActualizarProductoChangesStock() {
		when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));
		when(productoRepository.save(any(Producto.class))).thenReturn(producto);

		ProductoRequest updateRequest = new ProductoRequest("Teclado Pro", "RGB", new BigDecimal("79.99"), 15);
		ProductoResponse result = productoService.actualizar(1L, updateRequest);

		assertThat(result).isNotNull();
		verify(productoRepository).save(producto);

		ArgumentCaptor<ProductoDomainEvent> eventCaptor = ArgumentCaptor.forClass(ProductoDomainEvent.class);
		verify(applicationEventPublisher).publishEvent(eventCaptor.capture());
		assertThat(eventCaptor.getValue().getEventType()).isEqualTo(RabbitMQConfig.PRODUCTO_STOCK_ACTUALIZADO);
		assertThat(eventCaptor.getValue().getStock()).isEqualTo(15);
	}

	@Test
	void shouldRegisterUpdatedEventWhenActualizarProductoKeepsStock() {
		when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));
		when(productoRepository.save(any(Producto.class))).thenReturn(producto);

		ProductoRequest updateRequest = new ProductoRequest("Teclado Pro", "RGB", new BigDecimal("79.99"), 25);
		productoService.actualizar(1L, updateRequest);

		ArgumentCaptor<ProductoDomainEvent> eventCaptor = ArgumentCaptor.forClass(ProductoDomainEvent.class);
		verify(applicationEventPublisher).publishEvent(eventCaptor.capture());
		assertThat(eventCaptor.getValue().getEventType()).isEqualTo(RabbitMQConfig.PRODUCTO_ACTUALIZADO);
	}

	@Test
	void shouldEliminarLogicoProducto() {
		when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));
		when(productoRepository.save(any(Producto.class))).thenReturn(producto);

		productoService.eliminar(1L);

		assertThat(producto.getActivo()).isFalse();
		verify(productoRepository).save(producto);
		verify(applicationEventPublisher, never()).publishEvent(any());
	}

	@Test
	void shouldNotRegisterEventWhenCrearFails() {
		when(productoRepository.save(any(Producto.class))).thenThrow(new IllegalStateException("database failure"));

		assertThatThrownBy(() -> productoService.crear(request))
				.isInstanceOf(IllegalStateException.class);
		verify(applicationEventPublisher, never()).publishEvent(any());
	}

}
