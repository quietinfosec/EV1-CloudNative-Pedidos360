package cl.duoc.pedidos360.bff.config;

import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.jwt.JwtDecoder;

@TestConfiguration
public class TestSecurityConfig {

	@Bean
	@Primary
	public JwtDecoder jwtDecoder() {
		return Mockito.mock(JwtDecoder.class);
	}

}
