package cl.duoc.pedidos360.rabbitmqadmin;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import cl.duoc.pedidos360.rabbitmqadmin.client.RabbitManagementClient;
import cl.duoc.pedidos360.rabbitmqadmin.config.TestSecurityConfig;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestSecurityConfig.class)
class RabbitmqAdminServiceApplicationTests {

	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private RabbitManagementClient rabbitManagementClient;

	@Test
	void contextLoads() {
	}

	@Test
	void actuatorHealthEsPublico() throws Exception {
		mockMvc.perform(get("/actuator/health"))
				.andExpect(status().isOk());
	}

}
