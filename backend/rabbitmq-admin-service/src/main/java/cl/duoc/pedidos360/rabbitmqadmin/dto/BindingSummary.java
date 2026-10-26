package cl.duoc.pedidos360.rabbitmqadmin.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Binding entre un exchange y una cola/exchange, segun la Management API.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record BindingSummary(
		String source,
		String destination,
		@JsonProperty("destination_type") String destinationType,
		@JsonProperty("routing_key") String routingKey,
		String vhost,
		@JsonProperty("properties_key") String propertiesKey
) {

	public BindingSummary(String source, String destination, String destinationType, String routingKey, String vhost) {
		this(source, destination, destinationType, routingKey, vhost, null);
	}

}
