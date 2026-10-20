package cl.duoc.pedidos360.productos.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.test.web.servlet.MockMvc;

/**
 * EP3 (Fase 6): verifica el comportamiento del Resource Server ante tokens
 * invalidos y expirados (401) usando un {@link JwtDecoder} que rechaza esos
 * tokens; el token valido se sigue aceptando. El decoder se inyecta aqui para
 * no depender de un proveedor OIDC externo (Microsoft Entra ID).
 */
@SpringBootTest(properties = "pedidos360.security.require-auth=true")
@AutoConfigureMockMvc
@Import(JwtSecurityTests.RejectingDecoderConfig.class)
class JwtSecurityTests {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void tokenInvalidoRespondeNoAutorizado() throws Exception {
		mockMvc.perform(get("/api/productos").header("Authorization", "Bearer token-invalido"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void tokenExpiradoRespondeNoAutorizado() throws Exception {
		mockMvc.perform(get("/api/productos").header("Authorization", "Bearer token-expirado"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void tokenValidoAccede() throws Exception {
		mockMvc.perform(get("/api/productos").header("Authorization", "Bearer token-valido"))
				.andExpect(status().isOk());
	}

	@TestConfiguration
	static class RejectingDecoderConfig {

		@Bean
		@Primary
		JwtDecoder rejectingJwtDecoder() {
			return token -> {
				if ("token-valido".equals(token)) {
					return Jwt.withTokenValue(token)
							.header("alg", "none")
							.claim("sub", "usuario@duoc.cl")
							.claim("scp", "productos.read")
							.build();
				}
				if ("token-expirado".equals(token)) {
					throw new InvalidBearerTokenException("Token expirado");
				}
				throw new InvalidBearerTokenException("Token invalido");
			};
		}
	}

}
