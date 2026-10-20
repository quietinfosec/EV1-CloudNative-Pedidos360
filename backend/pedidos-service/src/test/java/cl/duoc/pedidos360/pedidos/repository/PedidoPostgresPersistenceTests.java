package cl.duoc.pedidos360.pedidos.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

import cl.duoc.pedidos360.pedidos.entity.EstadoPedido;
import cl.duoc.pedidos360.pedidos.entity.Pedido;

/**
 * EP3 (Fase 7): verifica persistencia real contra PostgreSQL (contenedor
 * pedidos360-postgres, host 55432), no H2. Se ejecuta solo con el tag
 * "postgres" para no exigir el contenedor en la corrida estandar:
 *
 * <pre>.\mvnw.cmd -B test -Dgroups=postgres -DexcludedGroups=</pre>
 *
 * Requiere el contenedor arriba (docker compose up -d postgres) y
 * DB_PORT/DB_PASSWORD acordes (por defecto localhost:55432, dbadmin/dbadmin).
 */
@Tag("postgres")
@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
@TestPropertySource(properties = {
		"spring.datasource.url=jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:55432}/${DB_NAME:pedidos360}",
		"spring.datasource.username=${DB_USERNAME:dbadmin}",
		"spring.datasource.password=${DB_PASSWORD:dbadmin}",
		"spring.datasource.driver-class-name=org.postgresql.Driver",
		"spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect",
		"spring.jpa.hibernate.ddl-auto=update"
})
class PedidoPostgresPersistenceTests {

	@Autowired
	private PedidoRepository pedidoRepository;

	@Test
	void persisteYRecuperaUnPedidoEnPostgres() {
		Pedido pedido = pedidoRepository.save(new Pedido(null, "pg@duoc.cl", LocalDateTime.now(),
				EstadoPedido.PENDIENTE, new BigDecimal("999.99")));

		assertThat(pedido.getId()).isNotNull();

		Optional<Pedido> found = pedidoRepository.findById(pedido.getId());
		assertThat(found).isPresent();
		assertThat(found.get().getUsuario()).isEqualTo("pg@duoc.cl");
		assertThat(found.get().getTotal()).isEqualByComparingTo("999.99");
		assertThat(found.get().getCreatedAt()).isNotNull();

		pedidoRepository.deleteById(pedido.getId());
		assertThat(pedidoRepository.findById(pedido.getId())).isEmpty();
	}

}
