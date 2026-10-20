package cl.duoc.pedidos360.bff.config;

import java.io.IOException;

import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

/**
 * Propaga el token JWT del usuario autenticado (Authorization: Bearer) hacia los
 * servicios internos, tomandolo del {@link SecurityContextHolder}. De esta forma
 * los servicios internos validan el mismo token que autentico el BFF (Zero Trust).
 */
@Component
public class BearerTokenPropagationInterceptor implements ClientHttpRequestInterceptor {

	@Override
	public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
			throws IOException {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication instanceof JwtAuthenticationToken jwtAuthentication) {
			request.getHeaders().setBearerAuth(jwtAuthentication.getToken().getTokenValue());
		}
		return execution.execute(request, body);
	}

}
