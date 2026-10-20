package cl.duoc.pedidos360.rabbitmqadmin.client;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import cl.duoc.pedidos360.rabbitmqadmin.dto.BindingSummary;
import cl.duoc.pedidos360.rabbitmqadmin.dto.ExchangeSummary;
import cl.duoc.pedidos360.rabbitmqadmin.dto.QueueSummary;
import cl.duoc.pedidos360.rabbitmqadmin.exception.RabbitManagementException;

@Component
public class RabbitManagementHttpClient implements RabbitManagementClient {

	private static final String DLQ_SUFFIX = ".dlq";

	private final RestClient rabbitManagementRestClient;

	public RabbitManagementHttpClient(RestClient rabbitManagementRestClient) {
		this.rabbitManagementRestClient = rabbitManagementRestClient;
	}

	@Override
	public List<QueueSummary> listQueues() {
		return getList("/queues", QueueSummary[].class);
	}

	@Override
	public List<ExchangeSummary> listExchanges() {
		return getList("/exchanges", ExchangeSummary[].class);
	}

	@Override
	public List<BindingSummary> listBindings() {
		return getList("/bindings", BindingSummary[].class);
	}

	@Override
	public List<QueueSummary> listDeadLetterQueues() {
		return listQueues().stream()
				.filter(queue -> queue.name() != null
						&& queue.name().toLowerCase(Locale.ROOT).endsWith(DLQ_SUFFIX))
				.toList();
	}

	private <T> List<T> getList(String path, Class<T[]> type) {
		try {
			T[] body = rabbitManagementRestClient.get()
					.uri(path)
					.retrieve()
					.body(type);
			return body == null ? List.of() : Arrays.asList(body);
		}
		catch (RestClientException ex) {
			throw new RabbitManagementException(
					"No fue posible consultar la Management API de RabbitMQ en " + path, ex);
		}
	}

}
