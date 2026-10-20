package cl.duoc.pedidos360.productos.event;

/**
 * EP3 (4.4): puerto de publicacion de eventos de productos.
 *
 * El dominio depende de esta abstraccion. La implementacion activa
 * (RabbitMQ o SQS/ElasticMQ) se selecciona con {@code messaging.provider}.
 */
public interface ProductoPublisher {

	void publishProductoCreado(ProductoEvent event);

	void publishProductoActualizado(ProductoEvent event);

	void publishStockActualizado(ProductoEvent event);

	void publishProductoError(ProductoEvent event);

}
