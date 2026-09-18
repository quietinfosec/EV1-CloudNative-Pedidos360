package cl.duoc.pedidos360.productos.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import cl.duoc.pedidos360.productos.entity.Producto;

@Repository
public interface ProductoRepository extends JpaRepository<Producto, Long> {

	List<Producto> findByActivoTrue();

	Optional<Producto> findByIdAndActivoTrue(Long id);

	boolean existsByNombreIgnoreCase(String nombre);

}
