package cl.duoc.pedidos360.rabbitmqadmin.client;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import cl.duoc.pedidos360.rabbitmqadmin.dto.BindingSummary;
import cl.duoc.pedidos360.rabbitmqadmin.dto.CreateBindingRequest;
import cl.duoc.pedidos360.rabbitmqadmin.dto.CreateExchangeRequest;
import cl.duoc.pedidos360.rabbitmqadmin.dto.CreateQueueRequest;
import cl.duoc.pedidos360.rabbitmqadmin.dto.ExchangeSummary;
import cl.duoc.pedidos360.rabbitmqadmin.dto.PublishMessageRequest;
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

	@Override
	public void createQueue(String vhost, CreateQueueRequest request) {
		post("/queues/{vhost}/{name}", Map.of(
				"durable", request.durable(),
				"auto_delete", request.autoDelete(),
				"arguments", request.arguments()
		), vhost, request.name());
	}

	@Override
	public void deleteQueue(String vhost, String name) {
		delete("/queues/{vhost}/{name}", vhost, name);
	}

	@Override
	public void purgeQueue(String vhost, String name) {
		delete("/queues/{vhost}/{name}/contents", vhost, name);
	}

	@Override
	public void createExchange(String vhost, CreateExchangeRequest request) {
		post("/exchanges/{vhost}/{name}", Map.of(
				"type", request.type(),
				"durable", request.durable(),
				"auto_delete", request.autoDelete(),
				"internal", request.internal(),
				"arguments", request.arguments()
		), vhost, request.name());
	}

	@Override
	public void deleteExchange(String vhost, String name) {
		delete("/exchanges/{vhost}/{name}", vhost, name);
	}

	@Override
	public void createBinding(String vhost, CreateBindingRequest request) {
		post("/bindings/{vhost}/e/{exchange}/q/{queue}", Map.of(
				"routing_key", request.routingKey(),
				"arguments", request.arguments()
		), vhost, request.exchange(), request.queue());
	}

	@Override
	public void deleteBinding(String vhost, String exchange, String queue, String routingKey) {
		BindingSummary binding = listBindings().stream()
				.filter(candidate -> Objects.equals(vhost, candidate.vhost()))
				.filter(candidate -> Objects.equals(exchange, candidate.source()))
				.filter(candidate -> Objects.equals(queue, candidate.destination()))
				.filter(candidate -> Objects.equals(routingKey, candidate.routingKey()))
				.findFirst()
				.orElseThrow(() -> new RabbitManagementException(
						"No existe el binding " + exchange + " -> " + queue + " con routingKey " + routingKey));
		if (binding.propertiesKey() == null || binding.propertiesKey().isBlank()) {
			throw new RabbitManagementException("El binding no expone properties_key");
		}
		delete("/bindings/{vhost}/e/{exchange}/q/{queue}/{propsKey}",
				vhost, exchange, queue, binding.propertiesKey());
	}

	@Override
	@SuppressWarnings("rawtypes")
	public boolean publish(String vhost, PublishMessageRequest request) {
		try {
			Map response = rabbitManagementRestClient.post()
					.uri("/exchanges/{vhost}/{exchange}/publish", vhost, request.exchange())
					.contentType(MediaType.APPLICATION_JSON)
					.body(Map.of(
							"properties", Map.of(),
							"routing_key", request.routingKey(),
							"payload", request.payload(),
							"payload_encoding", "string"
					))
					.retrieve()
					.body(Map.class);
			return response != null && Boolean.TRUE.equals(response.get("routed"));
		}
		catch (RestClientException ex) {
			throw new RabbitManagementException(
					"No fue posible publicar el mensaje en el exchange " + request.exchange(), ex);
		}
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

	private void post(String path, Object body, Object... uriVariables) {
		try {
			rabbitManagementRestClient.post()
					.uri(path, uriVariables)
					.contentType(MediaType.APPLICATION_JSON)
					.body(body)
					.retrieve()
					.toBodilessEntity();
		}
		catch (RestClientException ex) {
			throw new RabbitManagementException(
					"No fue posible crear el recurso en la Management API: " + path, ex);
		}
	}

	private void delete(String path, Object... uriVariables) {
		try {
			rabbitManagementRestClient.delete()
					.uri(path, uriVariables)
					.retrieve()
					.toBodilessEntity();
		}
		catch (RestClientException ex) {
			throw new RabbitManagementException(
					"No fue posible eliminar el recurso en la Management API: " + path, ex);
		}
	}

}
