package cl.duoc.pedidos360.pedidos;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "SPRING_PROFILES_ACTIVE=aws")
class PedidosServiceAwsProfileTests extends PedidosServiceApplicationTests {

	@Test
	void awsProfileKeepsTheServiceInternalWithoutRds() {
		assertThat(environment.getActiveProfiles()).containsExactly("aws");
		assertThat(environment.getProperty("server.address")).isEqualTo("127.0.0.1");
		assertThat(environment.getProperty("management.endpoints.web.exposure.include")).isEqualTo("health");
		assertThat(environment.getProperty("management.endpoint.health.show-details")).isEqualTo("never");
		assertThat(environment.containsProperty("spring.datasource.url")).isFalse();
		assertThat(environment.containsProperty("spring.datasource.username")).isFalse();
		assertThat(environment.containsProperty("spring.datasource.password")).isFalse();
	}

}
