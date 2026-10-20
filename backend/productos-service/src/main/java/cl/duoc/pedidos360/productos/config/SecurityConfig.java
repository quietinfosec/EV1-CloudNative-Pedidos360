package cl.duoc.pedidos360.productos.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Seguridad extremo a extremo (EP3 - 4.5).
 *
 * El servicio interno se comporta como OAuth2 Resource Server y rechaza con 401
 * toda llamada sin un JWT valido propagado por el BFF. El perfil local activa de
 * forma explicita un modo permisivo (property pedidos360.security.require-auth=false)
 * para no romper el desarrollo sin Microsoft Entra ID.
 */
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

	/**
	 * Modo seguro (por defecto): Resource Server. Exige JWT valido salvo health.
	 */
	@Bean
	@Order(1)
	@ConditionalOnProperty(name = "pedidos360.security.require-auth", havingValue = "true", matchIfMissing = true)
	public SecurityFilterChain securedSecurityFilterChain(HttpSecurity http) throws Exception {
		http
				.securityMatcher("/**")
				.csrf(AbstractHttpConfigurer::disable)
				.cors(Customizer.withDefaults())
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(auth -> auth
						.requestMatchers("/api/health", "/actuator/health", "/actuator/info").permitAll()
						.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
						.anyRequest().authenticated()
				)
				.oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));

		return http.build();
	}

	/**
	 * Modo local permisivo explicito (pedidos360.security.require-auth=false).
	 */
	@Bean
	@Order(2)
	@ConditionalOnProperty(name = "pedidos360.security.require-auth", havingValue = "false")
	public SecurityFilterChain permissiveSecurityFilterChain(HttpSecurity http) throws Exception {
		http
				.securityMatcher("/**")
				.csrf(AbstractHttpConfigurer::disable)
				.cors(Customizer.withDefaults())
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(auth -> auth.anyRequest().permitAll());

		return http.build();
	}

}
