package cl.duoc.pedidos360.rabbitmqadmin.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import cl.duoc.pedidos360.rabbitmqadmin.client.RabbitManagementClient;
import cl.duoc.pedidos360.rabbitmqadmin.config.TestSecurityConfig;
import cl.duoc.pedidos360.rabbitmqadmin.dto.BindingSummary;
import cl.duoc.pedidos360.rabbitmqadmin.dto.CreateBindingRequest;
import cl.duoc.pedidos360.rabbitmqadmin.dto.CreateExchangeRequest;
import cl.duoc.pedidos360.rabbitmqadmin.dto.CreateQueueRequest;
import cl.duoc.pedidos360.rabbitmqadmin.dto.ExchangeSummary;
import cl.duoc.pedidos360.rabbitmqadmin.dto.PublishMessageRequest;
import cl.duoc.pedidos360.rabbitmqadmin.dto.QueueSummary;
import cl.duoc.pedidos360.rabbitmqadmin.exception.RabbitManagementException;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestSecurityConfig.class)
class AdminControllerTests {

	private static final SimpleGrantedAuthority ADMIN = new SimpleGrantedAuthority("ROLE_ADMIN");

	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private RabbitManagementClient rabbitManagementClient;

	@Test
	void listaColasParaAdmin() throws Exception {
		given(rabbitManagementClient.listQueues()).willReturn(List.of(
				new QueueSummary("pedidos360.pedidos.cola", true, 3, 1, "running")));

		mockMvc.perform(get("/admin/queues").with(jwt().authorities(ADMIN)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].name").value("pedidos360.pedidos.cola"))
				.andExpect(jsonPath("$[0].messages").value(3))
				.andExpect(jsonPath("$[0].durable").value(true));
	}

	@Test
	void listaExchangesParaAdmin() throws Exception {
		given(rabbitManagementClient.listExchanges()).willReturn(List.of(
				new ExchangeSummary("pedidos360.topic.exchange", "topic", true)));

		mockMvc.perform(get("/admin/exchanges").with(jwt().authorities(ADMIN)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].name").value("pedidos360.topic.exchange"))
				.andExpect(jsonPath("$[0].type").value("topic"))
				.andExpect(jsonPath("$[0].durable").value(true));
	}

	@Test
	void listaBindingsParaAdmin() throws Exception {
		given(rabbitManagementClient.listBindings()).willReturn(List.of(
				new BindingSummary("pedidos360.topic.exchange", "pedidos360.pedidos.cola", "queue", "pedidos.pedido.#", "/")));

		mockMvc.perform(get("/admin/bindings").with(jwt().authorities(ADMIN)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].source").value("pedidos360.topic.exchange"))
				.andExpect(jsonPath("$[0].destination").value("pedidos360.pedidos.cola"))
				.andExpect(jsonPath("$[0].destination_type").value("queue"))
				.andExpect(jsonPath("$[0].routing_key").value("pedidos.pedido.#"));
	}

	@Test
	void listaColasDeadLetterParaAdmin() throws Exception {
		given(rabbitManagementClient.listDeadLetterQueues()).willReturn(List.of(
				new QueueSummary("pedidos360.pedidos.dlq", true, 1, 0, "running")));

		mockMvc.perform(get("/admin/dlq").with(jwt().authorities(ADMIN)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].name").value("pedidos360.pedidos.dlq"))
				.andExpect(jsonPath("$[0].messages").value(1));
	}

	@Test
	void respondeBadGatewayCuandoFallaLaManagementApi() throws Exception {
		given(rabbitManagementClient.listQueues())
				.willThrow(new RabbitManagementException("Management API no disponible"));

		mockMvc.perform(get("/admin/queues").with(jwt().authorities(ADMIN)))
				.andExpect(status().isBadGateway())
				.andExpect(jsonPath("$.error").value("rabbitmq_management_unavailable"));
	}

	@Test
	void creaColaParaAdmin() throws Exception {
		mockMvc.perform(post("/admin/queues")
						.with(jwt().authorities(ADMIN))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"name":"pedidos360.prueba.cola","durable":true}
								"""))
				.andExpect(status().isCreated());

		verify(rabbitManagementClient).createQueue(eq("/"), any(CreateQueueRequest.class));
	}

	@Test
	void rechazaColaSinNombre() throws Exception {
		mockMvc.perform(post("/admin/queues")
						.with(jwt().authorities(ADMIN))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"name":"   "}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("validacion_invalida"))
				.andExpect(jsonPath("$.fields.name").exists());

		verifyNoInteractions(rabbitManagementClient);
	}

	@Test
	void rechazaColaConNombreReservado() throws Exception {
		mockMvc.perform(post("/admin/queues")
						.with(jwt().authorities(ADMIN))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"name":"amq.pedidos360"}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("validacion_invalida"))
				.andExpect(jsonPath("$.fields.name").exists());

		verifyNoInteractions(rabbitManagementClient);
	}

	@Test
	void eliminaColaParaAdmin() throws Exception {
		mockMvc.perform(delete("/admin/queues/pedidos360.prueba.cola")
						.with(jwt().authorities(ADMIN)))
				.andExpect(status().isNoContent());

		verify(rabbitManagementClient).deleteQueue("/", "pedidos360.prueba.cola");
	}

	@Test
	void purgaColaParaAdmin() throws Exception {
		mockMvc.perform(delete("/admin/queues/pedidos360.prueba.cola/messages")
						.with(jwt().authorities(ADMIN)))
				.andExpect(status().isNoContent());

		verify(rabbitManagementClient).purgeQueue("/", "pedidos360.prueba.cola");
	}

	@Test
	void creaExchangeParaAdmin() throws Exception {
		mockMvc.perform(post("/admin/exchanges")
						.with(jwt().authorities(ADMIN))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"name":"pedidos360.prueba.exchange","type":"topic"}
								"""))
				.andExpect(status().isCreated());

		verify(rabbitManagementClient).createExchange(eq("/"), any(CreateExchangeRequest.class));
	}

	@Test
	void rechazaExchangeConTipoInvalido() throws Exception {
		mockMvc.perform(post("/admin/exchanges")
						.with(jwt().authorities(ADMIN))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"name":"pedidos360.prueba.exchange","type":"inexistente"}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("validacion_invalida"));

		verifyNoInteractions(rabbitManagementClient);
	}

	@Test
	void creaBindingParaAdmin() throws Exception {
		mockMvc.perform(post("/admin/bindings")
						.with(jwt().authorities(ADMIN))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"exchange":"pedidos360.topic.exchange","queue":"pedidos360.prueba.cola","routingKey":"pedidos.prueba.#"}
								"""))
				.andExpect(status().isCreated());

		verify(rabbitManagementClient).createBinding(eq("/"), any(CreateBindingRequest.class));
	}

	@Test
	void eliminaBindingParaAdmin() throws Exception {
		given(rabbitManagementClient.listBindings()).willReturn(List.of(
				new BindingSummary(
						"pedidos360.topic.exchange",
						"pedidos360.prueba.cola",
						"queue",
						"pedidos.prueba.#",
						"/",
						"pedidos.prueba.#")));

		mockMvc.perform(delete("/admin/bindings")
						.with(jwt().authorities(ADMIN))
						.param("exchange", "pedidos360.topic.exchange")
						.param("queue", "pedidos360.prueba.cola")
						.param("routingKey", "pedidos.prueba.#"))
				.andExpect(status().isNoContent());

		verify(rabbitManagementClient).deleteBinding(
				"/", "pedidos360.topic.exchange", "pedidos360.prueba.cola", "pedidos.prueba.#");
	}

	@Test
	void publicaMensajeParaAdmin() throws Exception {
		given(rabbitManagementClient.publish(eq("/"), any(PublishMessageRequest.class))).willReturn(true);

		mockMvc.perform(post("/admin/messages")
						.with(jwt().authorities(ADMIN))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"exchange":"pedidos360.topic.exchange","routingKey":"pedidos.pedido.creado","payload":"{}"}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.routed").value(true));

		verify(rabbitManagementClient).publish(eq("/"), any(PublishMessageRequest.class));
	}

}
