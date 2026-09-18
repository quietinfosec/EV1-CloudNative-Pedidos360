package cl.duoc.pedidos360.productos;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles(profiles = "aws", inheritProfiles = false)
class ProductosServiceAwsProfileTests extends ProductosServiceApplicationTests {

	@Test
	void awsProfileKeepsTheServiceInternalWithRds() {
		assertThat(environment.getActiveProfiles()).containsExactly("aws");
		assertThat(environment.getProperty("server.address")).isEqualTo("127.0.0.1");
		assertThat(environment.getProperty("management.endpoints.web.exposure.include")).isEqualTo("health");
		assertThat(environment.getProperty("management.endpoint.health.show-details")).isEqualTo("never");
		assertThat(environment.containsProperty("spring.datasource.url")).isTrue();
		assertThat(environment.containsProperty("spring.datasource.username")).isTrue();
		assertThat(environment.containsProperty("spring.datasource.password")).isTrue();
	}

}
