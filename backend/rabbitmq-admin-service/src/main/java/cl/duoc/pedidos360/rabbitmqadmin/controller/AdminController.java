package cl.duoc.pedidos360.rabbitmqadmin.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cl.duoc.pedidos360.rabbitmqadmin.client.RabbitManagementClient;
import cl.duoc.pedidos360.rabbitmqadmin.dto.BindingSummary;
import cl.duoc.pedidos360.rabbitmqadmin.dto.ExchangeSummary;
import cl.duoc.pedidos360.rabbitmqadmin.dto.QueueSummary;

/**
 * Panel de gestion y observabilidad de RabbitMQ.
 *
 * Requiere rol ADMIN (ver SecurityConfig). Las credenciales del broker nunca
 * se exponen al cliente: el servicio las usa server-side contra la Management API.
 */
@RestController
@RequestMapping("/admin")
public class AdminController {

	private final RabbitManagementClient rabbitManagementClient;

	public AdminController(RabbitManagementClient rabbitManagementClient) {
		this.rabbitManagementClient = rabbitManagementClient;
	}

	@GetMapping("/queues")
	public List<QueueSummary> queues() {
		return rabbitManagementClient.listQueues();
	}

	@GetMapping("/exchanges")
	public List<ExchangeSummary> exchanges() {
		return rabbitManagementClient.listExchanges();
	}

	@GetMapping("/bindings")
	public List<BindingSummary> bindings() {
		return rabbitManagementClient.listBindings();
	}

	@GetMapping("/dlq")
	public List<QueueSummary> deadLetterQueues() {
		return rabbitManagementClient.listDeadLetterQueues();
	}

}
