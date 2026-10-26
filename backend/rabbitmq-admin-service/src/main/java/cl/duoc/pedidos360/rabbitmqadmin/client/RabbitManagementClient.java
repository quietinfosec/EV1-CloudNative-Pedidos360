package cl.duoc.pedidos360.rabbitmqadmin.client;

import java.util.List;

import cl.duoc.pedidos360.rabbitmqadmin.dto.BindingSummary;
import cl.duoc.pedidos360.rabbitmqadmin.dto.CreateBindingRequest;
import cl.duoc.pedidos360.rabbitmqadmin.dto.CreateExchangeRequest;
import cl.duoc.pedidos360.rabbitmqadmin.dto.CreateQueueRequest;
import cl.duoc.pedidos360.rabbitmqadmin.dto.ExchangeSummary;
import cl.duoc.pedidos360.rabbitmqadmin.dto.PublishMessageRequest;
import cl.duoc.pedidos360.rabbitmqadmin.dto.QueueSummary;

/**
 * Puerto de acceso a la Management API de RabbitMQ. Aisla al controlador de la
 * tecnologia HTTP concreta y permite sustituirla en pruebas.
 */
public interface RabbitManagementClient {

	List<QueueSummary> listQueues();

	List<ExchangeSummary> listExchanges();

	List<BindingSummary> listBindings();

	List<QueueSummary> listDeadLetterQueues();

	void createQueue(String vhost, CreateQueueRequest request);

	void deleteQueue(String vhost, String name);

	void purgeQueue(String vhost, String name);

	void createExchange(String vhost, CreateExchangeRequest request);

	void deleteExchange(String vhost, String name);

	void createBinding(String vhost, CreateBindingRequest request);

	void deleteBinding(String vhost, String exchange, String queue, String routingKey);

	boolean publish(String vhost, PublishMessageRequest request);

}
