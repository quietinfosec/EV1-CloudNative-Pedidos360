package cl.duoc.pedidos360.rabbitmqadmin.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(RabbitManagementProperties.class)
public class RabbitManagementClientConfig {

	@Bean
	public RestClient rabbitManagementRestClient(RabbitManagementProperties properties) {
		return RestClient.builder()
				.baseUrl(properties.getBaseUrl())
				.defaultHeaders(headers -> headers.setBasicAuth(
						properties.getUsername(),
						properties.getPassword()))
				.build();
	}

}
