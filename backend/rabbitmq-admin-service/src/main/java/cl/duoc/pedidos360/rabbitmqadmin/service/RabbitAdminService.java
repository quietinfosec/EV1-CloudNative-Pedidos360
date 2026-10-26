package cl.duoc.pedidos360.rabbitmqadmin.service;

import java.util.List;

import org.springframework.stereotype.Service;

import cl.duoc.pedidos360.rabbitmqadmin.client.RabbitManagementClient;
import cl.duoc.pedidos360.rabbitmqadmin.dto.BindingSummary;
import cl.duoc.pedidos360.rabbitmqadmin.dto.CreateBindingRequest;
import cl.duoc.pedidos360.rabbitmqadmin.dto.CreateExchangeRequest;
import cl.duoc.pedidos360.rabbitmqadmin.dto.CreateQueueRequest;
import cl.duoc.pedidos360.rabbitmqadmin.dto.ExchangeSummary;
import cl.duoc.pedidos360.rabbitmqadmin.dto.PublishMessageRequest;
import cl.duoc.pedidos360.rabbitmqadmin.dto.QueueSummary;

/**
 * EP3: caso de uso central del panel de administracion.
 *
 * Concentra las operaciones de lectura y mutacion sobre RabbitMQ. El
 * controlador solo traduce HTTP a este servicio y el cliente HTTP solo conoce
 * la Management API; no hay logica de negocio repartida entre capas.
 */
@Service
public class RabbitAdminService {

	private final RabbitManagementClient rabbitManagementClient;

	public RabbitAdminService(RabbitManagementClient rabbitManagementClient) {
		this.rabbitManagementClient = rabbitManagementClient;
	}

	public List<QueueSummary> listQueues() {
		return rabbitManagementClient.listQueues();
	}

	public List<ExchangeSummary> listExchanges() {
		return rabbitManagementClient.listExchanges();
	}

	public List<BindingSummary> listBindings() {
		return rabbitManagementClient.listBindings();
	}

	public List<QueueSummary> listDeadLetterQueues() {
		return rabbitManagementClient.listDeadLetterQueues();
	}

	public void createQueue(CreateQueueRequest request) {
		rabbitManagementClient.createQueue(request.vhost(), request);
	}

	public void deleteQueue(String vhost, String name) {
		requireText(name, "name");
		rabbitManagementClient.deleteQueue(vhost, name);
	}

	public void purgeQueue(String vhost, String name) {
		requireText(name, "name");
		rabbitManagementClient.purgeQueue(vhost, name);
	}

	public void createExchange(CreateExchangeRequest request) {
		rabbitManagementClient.createExchange(request.vhost(), request);
	}

	public void deleteExchange(String vhost, String name) {
		requireText(name, "name");
		rabbitManagementClient.deleteExchange(vhost, name);
	}

	public void createBinding(CreateBindingRequest request) {
		rabbitManagementClient.createBinding(request.vhost(), request);
	}

	public void deleteBinding(String vhost, String exchange, String queue, String routingKey) {
		requireText(exchange, "exchange");
		requireText(queue, "queue");
		rabbitManagementClient.deleteBinding(vhost, exchange, queue, routingKey);
	}

	public boolean publish(PublishMessageRequest request) {
		return rabbitManagementClient.publish(request.vhost(), request);
	}

	private void requireText(String value, String field) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException(field + " es obligatorio");
		}
	}

}
