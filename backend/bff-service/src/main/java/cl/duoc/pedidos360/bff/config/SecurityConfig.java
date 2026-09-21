package cl.duoc.pedidos360.bff.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

	@Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri:}")
	private String jwkSetUri;

	@Bean
	@ConditionalOnMissingBean(JwtDecoder.class)
	@ConditionalOnProperty(name = "spring.security.oauth2.resourceserver.jwt.jwk-set-uri")
	public JwtDecoder jwtDecoder() {
		if (jwkSetUri != null && !jwkSetUri.isBlank()) {
			return NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
		}
		return null;
	}

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
				.csrf(AbstractHttpConfigurer::disable)
				.cors(Customizer.withDefaults())
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(auth -> auth
						// Health y Actuator publicos
						.requestMatchers("/api/health", "/actuator/health", "/actuator/info").permitAll()
						.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
						// Consultas GET de catalogo publicas
						.requestMatchers(HttpMethod.GET, "/api/productos", "/api/productos/**").permitAll()
						// Operaciones de escritura y pedidos protegidas
						.requestMatchers(HttpMethod.POST, "/api/productos").authenticated()
						.requestMatchers(HttpMethod.PUT, "/api/productos/**").authenticated()
						.requestMatchers(HttpMethod.DELETE, "/api/productos/**").authenticated()
						.requestMatchers("/api/pedidos", "/api/pedidos/**").authenticated()
						.anyRequest().authenticated()
				)
				.oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));

		return http.build();
	}

}
