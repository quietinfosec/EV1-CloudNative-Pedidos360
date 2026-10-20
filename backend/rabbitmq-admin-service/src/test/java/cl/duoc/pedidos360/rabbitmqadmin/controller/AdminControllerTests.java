package cl.duoc.pedidos360.rabbitmqadmin.controller;

import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import cl.duoc.pedidos360.rabbitmqadmin.client.RabbitManagementClient;
import cl.duoc.pedidos360.rabbitmqadmin.config.TestSecurityConfig;
import cl.duoc.pedidos360.rabbitmqadmin.dto.BindingSummary;
import cl.duoc.pedidos360.rabbitmqadmin.dto.ExchangeSummary;
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

}
