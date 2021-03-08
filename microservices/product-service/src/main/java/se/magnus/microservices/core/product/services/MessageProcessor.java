package se.magnus.microservices.core.product.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.cloud.stream.messaging.Sink;
import se.magnus.api.core.product.Product;
import se.magnus.api.core.product.ProductService;
import se.magnus.api.event.Event;
import se.magnus.util.exceptions.EventProcessingException;

@EnableBinding(Sink.class)
@Slf4j
public class MessageProcessor {
    private final ProductService ProductService;

    @Autowired
    public MessageProcessor(ProductService ProductService) {
        this.ProductService = ProductService;
    }

    @StreamListener(target = Sink.INPUT)
    public void process(Event<Integer, Product> event) {
        log.info("process message created at {}...", event.getEventCreatedAt());

        switch (event.getEventType()) {
            case CREATE:
                Product Product = event.getData();
                log.info("create Product with id : {}", Product.getProductId());
                ProductService.createProduct(Product);
                break;

            case DELETE:
                int productId = event.getKey();
                log.info("delete Product with id : {}", productId);
                ProductService.deleteProduct(productId);
                break;

            default:
                String errMsg = "incorrect event type: " + event.getEventType();
                log.warn(errMsg);
                throw new EventProcessingException(errMsg);
        }

        log.info("message processing done");
    }
}
