package cl.duoc.pedidos360.bff.config;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class BffClientConfig {

	@Value("${productos-service.url:http://localhost:8082}")
	private String productosServiceUrl;

	@Value("${pedidos-service.url:http://localhost:8081}")
	private String pedidosServiceUrl;

	private SimpleClientHttpRequestFactory createRequestFactory() {
		SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
		factory.setConnectTimeout((int) Duration.ofSeconds(3).toMillis());
		factory.setReadTimeout((int) Duration.ofSeconds(5).toMillis());
		return factory;
	}

	@Bean
	public RestClient productosRestClient() {
		return RestClient.builder()
				.baseUrl(productosServiceUrl)
				.requestFactory(createRequestFactory())
				.build();
	}

	@Bean
	public RestClient pedidosRestClient() {
		return RestClient.builder()
				.baseUrl(pedidosServiceUrl)
				.requestFactory(createRequestFactory())
				.build();
	}

}
