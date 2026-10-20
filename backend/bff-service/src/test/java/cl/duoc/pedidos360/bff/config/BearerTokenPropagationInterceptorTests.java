package cl.duoc.pedidos360.bff.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.net.URI;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.mock.http.client.MockClientHttpResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class BearerTokenPropagationInterceptorTests {

	private final BearerTokenPropagationInterceptor interceptor = new BearerTokenPropagationInterceptor();

	@AfterEach
	void clearContext() {
		SecurityContextHolder.clearContext();
	}

	private void authenticate(String token) {
		Jwt jwt = Jwt.withTokenValue(token)
				.header("alg", "none")
				.claim("sub", "usuario@duoc.cl")
				.build();
		SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
	}

	private MockClientHttpResponse ok() {
		return new MockClientHttpResponse(new byte[0], 200);
	}

	@Test
	void propagaElTokenCuandoHayJwtAutenticado() throws Exception {
		authenticate("token-123");
		MockClientHttpRequest request = new MockClientHttpRequest(HttpMethod.GET,
				URI.create("http://localhost:8081/api/pedidos"));

		interceptor.intercept(request, new byte[0], (req, body) -> ok());

		assertThat(request.getHeaders().getFirst("Authorization")).isEqualTo("Bearer token-123");
	}

	@Test
	void noPropagaHeaderSinAutenticacion() throws Exception {
		MockClientHttpRequest request = new MockClientHttpRequest(HttpMethod.GET,
				URI.create("http://localhost:8081/api/pedidos"));

		interceptor.intercept(request, new byte[0], (req, body) -> ok());

		assertThat(request.getHeaders().containsKey("Authorization")).isFalse();
	}

	@Test
	void elRestClientReenviaElHeaderBearer() {
		authenticate("token-abc");
		RestClient.Builder builder = RestClient.builder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		RestClient client = builder.baseUrl("http://localhost:8081")
				.requestInterceptor(interceptor)
				.build();

		server.expect(requestTo("http://localhost:8081/api/pedidos"))
				.andExpect(header("Authorization", "Bearer token-abc"))
				.andRespond(withSuccess());

		client.get().uri("/api/pedidos").retrieve().toBodilessEntity();

		server.verify();
	}

}
