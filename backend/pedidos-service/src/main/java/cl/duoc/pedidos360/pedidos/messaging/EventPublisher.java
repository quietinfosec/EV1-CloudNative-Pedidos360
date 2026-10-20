package cl.duoc.pedidos360.pedidos.messaging;

/**
 * EP3 (4.4): puerto de publicacion de eventos.
 *
 * El dominio depende de esta abstraccion, no de un SDK concreto. La
 * implementacion activa se selecciona con la propiedad
 * {@code messaging.provider=rabbitmq|sqs} (RabbitMQ por defecto).
 */
public interface EventPublisher {

	void publish(String routingKey, Object payload);

}
