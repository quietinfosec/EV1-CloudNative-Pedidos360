package cl.duoc.pedidos360.rabbitmqadmin.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import cl.duoc.pedidos360.rabbitmqadmin.dto.BindingSummary;
import cl.duoc.pedidos360.rabbitmqadmin.dto.CreateQueueRequest;
import cl.duoc.pedidos360.rabbitmqadmin.dto.ExchangeSummary;
import cl.duoc.pedidos360.rabbitmqadmin.dto.PublishMessageRequest;
import cl.duoc.pedidos360.rabbitmqadmin.dto.QueueSummary;
import cl.duoc.pedidos360.rabbitmqadmin.exception.RabbitManagementException;

class RabbitManagementClientTests {

	private static final String BASE_URL = "http://localhost:15672/api";

	private MockRestServiceServer server;

	private RabbitManagementHttpClient client;

	@BeforeEach
	void setUp() {
		RestClient.Builder builder = RestClient.builder();
		server = MockRestServiceServer.bindTo(builder).build();
		client = new RabbitManagementHttpClient(builder.baseUrl(BASE_URL).build());
	}

	@Test
	void parseaColas() {
		server.expect(requestTo(BASE_URL + "/queues"))
				.andRespond(withSuccess("""
						[
						  {"name":"pedidos360.pedidos.cola","durable":true,"messages":5,"consumers":1,"state":"running"}
						]
						""", MediaType.APPLICATION_JSON));

		List<QueueSummary> queues = client.listQueues();

		assertThat(queues).hasSize(1);
		assertThat(queues.get(0).name()).isEqualTo("pedidos360.pedidos.cola");
		assertThat(queues.get(0).durable()).isTrue();
		assertThat(queues.get(0).messages()).isEqualTo(5);
		assertThat(queues.get(0).consumers()).isEqualTo(1);
		assertThat(queues.get(0).state()).isEqualTo("running");
		server.verify();
	}

	@Test
	void parseaExchanges() {
		server.expect(requestTo(BASE_URL + "/exchanges"))
				.andRespond(withSuccess("""
						[
						  {"name":"pedidos360.topic.exchange","type":"topic","durable":true}
						]
						""", MediaType.APPLICATION_JSON));

		List<ExchangeSummary> exchanges = client.listExchanges();

		assertThat(exchanges).hasSize(1);
		assertThat(exchanges.get(0).name()).isEqualTo("pedidos360.topic.exchange");
		assertThat(exchanges.get(0).type()).isEqualTo("topic");
		assertThat(exchanges.get(0).durable()).isTrue();
		server.verify();
	}

	@Test
	void parseaBindings() {
		server.expect(requestTo(BASE_URL + "/bindings"))
				.andRespond(withSuccess("""
						[
						  {"source":"pedidos360.topic.exchange","destination":"pedidos360.pedidos.cola",
						   "destination_type":"queue","routing_key":"pedidos.pedido.#","vhost":"/"}
						]
						""", MediaType.APPLICATION_JSON));

		List<BindingSummary> bindings = client.listBindings();

		assertThat(bindings).hasSize(1);
		assertThat(bindings.get(0).source()).isEqualTo("pedidos360.topic.exchange");
		assertThat(bindings.get(0).destination()).isEqualTo("pedidos360.pedidos.cola");
		assertThat(bindings.get(0).destinationType()).isEqualTo("queue");
		assertThat(bindings.get(0).routingKey()).isEqualTo("pedidos.pedido.#");
		server.verify();
	}

	@Test
	void filtraSoloColasDeadLetter() {
		server.expect(requestTo(BASE_URL + "/queues"))
				.andRespond(withSuccess("""
						[
						  {"name":"pedidos360.pedidos.cola","durable":true,"messages":0,"consumers":1,"state":"running"},
						  {"name":"pedidos360.pedidos.dlq","durable":true,"messages":2,"consumers":0,"state":"running"},
						  {"name":"pedidos360.auditoria.cola","durable":true,"messages":0,"consumers":1,"state":"running"}
						]
						""", MediaType.APPLICATION_JSON));

		List<QueueSummary> dlq = client.listDeadLetterQueues();

		assertThat(dlq).hasSize(1);
		assertThat(dlq.get(0).name()).isEqualTo("pedidos360.pedidos.dlq");
		assertThat(dlq.get(0).messages()).isEqualTo(2);
		server.verify();
	}

	@Test
	void lanzaExcepcionCuandoLaManagementApiFalla() {
		server.expect(requestTo(BASE_URL + "/queues"))
				.andRespond(withServerError());

		assertThatThrownBy(() -> client.listQueues())
				.isInstanceOf(RabbitManagementException.class);
		server.verify();
	}

	@Test
	void creaColaConVhostCodificado() {
		server.expect(requestTo(BASE_URL + "/queues/%2F/pedidos360.prueba.cola"))
				.andExpect(method(HttpMethod.POST))
				.andExpect(content().json("""
						{"durable":true,"auto_delete":false,"arguments":{}}
						"""))
				.andRespond(withSuccess());

		client.createQueue("/", new CreateQueueRequest(
				"pedidos360.prueba.cola", "/", true, false, Map.of()));

		server.verify();
	}

	@Test
	void eliminaColaConVhostCodificado() {
		server.expect(requestTo(BASE_URL + "/queues/%2F/pedidos360.prueba.cola"))
				.andExpect(method(HttpMethod.DELETE))
				.andRespond(withSuccess());

		client.deleteQueue("/", "pedidos360.prueba.cola");

		server.verify();
	}

	@Test
	void publicaMensajeYLeeRouted() {
		server.expect(requestTo(BASE_URL + "/exchanges/%2F/pedidos360.topic.exchange/publish"))
				.andExpect(method(HttpMethod.POST))
				.andExpect(content().json("""
						{"routing_key":"pedidos.pedido.creado","payload":"{}","payload_encoding":"string"}
						"""))
				.andRespond(withSuccess("""
						{"routed":true}
						""", MediaType.APPLICATION_JSON));

		boolean routed = client.publish("/", new PublishMessageRequest(
				"pedidos360.topic.exchange", "pedidos.pedido.creado", "{}", "/"));

		assertThat(routed).isTrue();
		server.verify();
	}

}
