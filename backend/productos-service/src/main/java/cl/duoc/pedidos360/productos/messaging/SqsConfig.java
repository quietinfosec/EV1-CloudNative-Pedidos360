package cl.duoc.pedidos360.productos.messaging;

import java.net.URI;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;

/**
 * EP3 (4.4): cliente AWS SQS v2 para el perfil {@code messaging.provider=sqs}.
 *
 * El endpoint es configurable para apuntar a ElasticMQ en local.
 */
@Configuration
@ConditionalOnProperty(name = "messaging.provider", havingValue = "sqs")
public class SqsConfig {

	@Value("${messaging.sqs.endpoint:http://localhost:9324}")
	private String endpoint;

	@Value("${messaging.sqs.region:us-east-1}")
	private String region;

	@Value("${messaging.sqs.access-key:elasticmq}")
	private String accessKey;

	@Value("${messaging.sqs.secret-key:elasticmq}")
	private String secretKey;

	@Bean
	public SqsClient sqsClient() {
		return SqsClient.builder()
				.endpointOverride(URI.create(endpoint))
				.region(Region.of(region))
				.credentialsProvider(StaticCredentialsProvider.create(
						AwsBasicCredentials.create(accessKey, secretKey)))
				.build();
	}

}
