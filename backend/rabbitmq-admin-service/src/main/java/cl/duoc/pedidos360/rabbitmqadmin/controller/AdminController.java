package cl.duoc.pedidos360.rabbitmqadmin.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import cl.duoc.pedidos360.rabbitmqadmin.dto.BindingSummary;
import cl.duoc.pedidos360.rabbitmqadmin.dto.CreateBindingRequest;
import cl.duoc.pedidos360.rabbitmqadmin.dto.CreateExchangeRequest;
import cl.duoc.pedidos360.rabbitmqadmin.dto.CreateQueueRequest;
import cl.duoc.pedidos360.rabbitmqadmin.dto.ExchangeSummary;
import cl.duoc.pedidos360.rabbitmqadmin.dto.PublishMessageRequest;
import cl.duoc.pedidos360.rabbitmqadmin.dto.QueueSummary;
import cl.duoc.pedidos360.rabbitmqadmin.service.RabbitAdminService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

/**
 * Panel de gestion y observabilidad de RabbitMQ.
 *
 * Requiere rol ADMIN (ver SecurityConfig). Las credenciales del broker nunca
 * se exponen al cliente: el servicio las usa server-side contra la Management API.
 */
@RestController
@RequestMapping("/admin")
@Validated
public class AdminController {

	private final RabbitAdminService rabbitAdminService;

	public AdminController(RabbitAdminService rabbitAdminService) {
		this.rabbitAdminService = rabbitAdminService;
	}

	@GetMapping("/queues")
	public List<QueueSummary> queues() {
		return rabbitAdminService.listQueues();
	}

	@GetMapping("/exchanges")
	public List<ExchangeSummary> exchanges() {
		return rabbitAdminService.listExchanges();
	}

	@GetMapping("/bindings")
	public List<BindingSummary> bindings() {
		return rabbitAdminService.listBindings();
	}

	@GetMapping("/dlq")
	public List<QueueSummary> deadLetterQueues() {
		return rabbitAdminService.listDeadLetterQueues();
	}

	@PostMapping("/queues")
	@ResponseStatus(HttpStatus.CREATED)
	public void createQueue(@Valid @RequestBody CreateQueueRequest request) {
		rabbitAdminService.createQueue(request);
	}

	@DeleteMapping("/queues/{name}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deleteQueue(
			@PathVariable @NotBlank String name,
			@RequestParam(defaultValue = "/") String vhost) {
		rabbitAdminService.deleteQueue(vhost, name);
	}

	@DeleteMapping("/queues/{name}/messages")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void purgeQueue(
			@PathVariable @NotBlank String name,
			@RequestParam(defaultValue = "/") String vhost) {
		rabbitAdminService.purgeQueue(vhost, name);
	}

	@PostMapping("/exchanges")
	@ResponseStatus(HttpStatus.CREATED)
	public void createExchange(@Valid @RequestBody CreateExchangeRequest request) {
		rabbitAdminService.createExchange(request);
	}

	@DeleteMapping("/exchanges/{name}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deleteExchange(
			@PathVariable @NotBlank String name,
			@RequestParam(defaultValue = "/") String vhost) {
		rabbitAdminService.deleteExchange(vhost, name);
	}

	@PostMapping("/bindings")
	@ResponseStatus(HttpStatus.CREATED)
	public void createBinding(@Valid @RequestBody CreateBindingRequest request) {
		rabbitAdminService.createBinding(request);
	}

	@DeleteMapping("/bindings")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deleteBinding(
			@RequestParam @NotBlank String exchange,
			@RequestParam @NotBlank String queue,
			@RequestParam(defaultValue = "") String routingKey,
			@RequestParam(defaultValue = "/") String vhost) {
		rabbitAdminService.deleteBinding(vhost, exchange, queue, routingKey);
	}

	@PostMapping("/messages")
	public Map<String, Object> publish(@Valid @RequestBody PublishMessageRequest request) {
		return Map.of("routed", rabbitAdminService.publish(request));
	}

}
