package cl.duoc.pedidos360.rabbitmqadmin;

import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestSecurityConfig.class)
class AdminSecurityTests {

	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private RabbitManagementClient rabbitManagementClient;

	@Test
	void sinTokenRespondeNoAutorizado() throws Exception {
		mockMvc.perform(get("/admin/queues"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void tokenSinRolAdminRespondeProhibido() throws Exception {
		mockMvc.perform(get("/admin/queues").with(jwt()))
				.andExpect(status().isForbidden());
	}

	@Test
	void tokenConRolAdminAccede() throws Exception {
		given(rabbitManagementClient.listQueues()).willReturn(List.of());

		mockMvc.perform(get("/admin/queues")
						.with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
				.andExpect(status().isOk());
	}

}
