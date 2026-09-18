package cl.duoc.pedidos360.bff;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.env.StandardEnvironment;

class BffServiceConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withPropertyValues("spring.config.location=classpath:/")
			.withInitializer(context -> {
				// Isolate configuration defaults from the developer machine or CI environment.
				context.getEnvironment().getPropertySources()
						.remove(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME);
				context.getEnvironment().getPropertySources()
						.remove(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME);
			})
			.withInitializer(new ConfigDataApplicationContextInitializer());

	@Test
	void defaultsToLocalProfile() {
		contextRunner.run(context -> {
			assertThat(context).hasNotFailed();
			assertThat(context.getEnvironment().getActiveProfiles()).containsExactly("local");
		});
	}

	@ParameterizedTest
	@ValueSource(strings = { "local", "aws" })
	void serviceUrlsHaveLocalDefaultsInBothProfiles(String profile) {
		contextRunner.withPropertyValues("SPRING_PROFILES_ACTIVE=" + profile).run(context -> {
			assertThat(context).hasNotFailed();
			assertThat(context.getEnvironment().getActiveProfiles()).containsExactly(profile);
			assertThat(context.getEnvironment().getProperty("server.port", Integer.class)).isEqualTo(8080);
			assertThat(context.getEnvironment().getProperty("pedidos-service.url"))
					.isEqualTo("http://localhost:8081");
			assertThat(context.getEnvironment().getProperty("productos-service.url"))
					.isEqualTo("http://localhost:8082");
		});
	}

	@ParameterizedTest
	@ValueSource(strings = { "local", "aws" })
	void serviceUrlsCanBeOverriddenInBothProfiles(String profile) {
		contextRunner.withPropertyValues(
				"SPRING_PROFILES_ACTIVE=" + profile,
				"PEDIDOS_SERVICE_URL=http://127.0.0.1:18081",
				"PRODUCTOS_SERVICE_URL=http://127.0.0.1:18082").run(context -> {
					assertThat(context).hasNotFailed();
					assertThat(context.getEnvironment().getActiveProfiles()).containsExactly(profile);
					assertThat(context.getEnvironment().getProperty("pedidos-service.url"))
							.isEqualTo("http://127.0.0.1:18081");
					assertThat(context.getEnvironment().getProperty("productos-service.url"))
							.isEqualTo("http://127.0.0.1:18082");
				});
	}

}
