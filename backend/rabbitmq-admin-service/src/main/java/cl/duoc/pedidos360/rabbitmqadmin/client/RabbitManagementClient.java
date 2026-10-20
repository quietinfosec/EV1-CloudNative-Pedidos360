package cl.duoc.pedidos360.rabbitmqadmin.client;

import java.util.List;

import cl.duoc.pedidos360.rabbitmqadmin.dto.BindingSummary;
import cl.duoc.pedidos360.rabbitmqadmin.dto.ExchangeSummary;
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

}
