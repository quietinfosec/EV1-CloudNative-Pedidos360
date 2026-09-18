package cl.duoc.pedidos360.productos.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import cl.duoc.pedidos360.productos.entity.Producto;

@DataJpaTest
class ProductoRepositoryTests {

	@Autowired
	private ProductoRepository productoRepository;

	private Producto producto;

	@BeforeEach
	void setUp() {
		productoRepository.deleteAll();
		producto = new Producto(null, "Notebook", "Notebook Gamer", new BigDecimal("999.99"), 10, true);
		producto = productoRepository.save(producto);
	}

	@Test
	void shouldSaveAndFindById() {
		Optional<Producto> found = productoRepository.findById(producto.getId());
		assertThat(found).isPresent();
		assertThat(found.get().getNombre()).isEqualTo("Notebook");
		assertThat(found.get().getActivo()).isTrue();
	}

	@Test
	void shouldFindByActivoTrue() {
		Producto inactivo = new Producto(null, "Teclado Viejo", "Roto", new BigDecimal("5.00"), 0, false);
		productoRepository.save(inactivo);

		List<Producto> activos = productoRepository.findByActivoTrue();
		assertThat(activos).hasSize(1);
		assertThat(activos.get(0).getNombre()).isEqualTo("Notebook");
	}

	@Test
	void shouldFindByIdAndActivoTrue() {
		Optional<Producto> found = productoRepository.findByIdAndActivoTrue(producto.getId());
		assertThat(found).isPresent();
	}

	@Test
	void shouldCheckExistsByNombreIgnoreCase() {
		assertThat(productoRepository.existsByNombreIgnoreCase("notebook")).isTrue();
		assertThat(productoRepository.existsByNombreIgnoreCase("inexistente")).isFalse();
	}

	@Test
	void shouldDeleteProducto() {
		productoRepository.deleteById(producto.getId());
		assertThat(productoRepository.findById(producto.getId())).isEmpty();
	}

}
