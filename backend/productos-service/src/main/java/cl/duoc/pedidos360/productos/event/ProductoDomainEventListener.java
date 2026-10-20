package cl.duoc.pedidos360.productos.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import cl.duoc.pedidos360.productos.config.RabbitMQConfig;

@Component
public class ProductoDomainEventListener {

	private static final Logger log = LoggerFactory.getLogger(ProductoDomainEventListener.class);

	private final ProductoPublisher publisher;

	public ProductoDomainEventListener(ProductoPublisher publisher) {
		this.publisher = publisher;
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void handle(ProductoDomainEvent domainEvent) {
		ProductoEvent event = toIntegrationEvent(domainEvent);
		String routingKey = domainEvent.getEventType();

		log.info(
				"Publicando evento RabbitMQ after-commit eventId={} eventType={} productoId={} routingKey={}",
				event.getEventId(),
				event.getEventType(),
				event.getProductoId(),
				routingKey
		);

		try {
			publish(event, routingKey);
		} catch (RuntimeException exception) {
			log.error(
					"No fue posible publicar evento after-commit eventId={} eventType={} productoId={} routingKey={}",
					event.getEventId(),
					event.getEventType(),
					event.getProductoId(),
					routingKey,
					exception
			);
		}
	}

	private ProductoEvent toIntegrationEvent(ProductoDomainEvent domainEvent) {
		return new ProductoEvent(
				domainEvent.getEventType(),
				domainEvent.getProductoId(),
				domainEvent.getNombre(),
				domainEvent.getStock(),
				domainEvent.getPrecio(),
				domainEvent.getCorrelationId()
		);
	}

	private void publish(ProductoEvent event, String routingKey) {
		switch (routingKey) {
			case RabbitMQConfig.PRODUCTO_CREADO -> publisher.publishProductoCreado(event);
			case RabbitMQConfig.PRODUCTO_ACTUALIZADO -> publisher.publishProductoActualizado(event);
			case RabbitMQConfig.PRODUCTO_STOCK_ACTUALIZADO -> publisher.publishStockActualizado(event);
			default -> throw new IllegalArgumentException("Routing key de producto no soportada: " + routingKey);
		}
	}

}
