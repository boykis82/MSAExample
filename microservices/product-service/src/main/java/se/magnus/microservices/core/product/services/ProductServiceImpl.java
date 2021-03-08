package se.magnus.microservices.core.product.services;

import lombok.extern.slf4j.Slf4j;

import org.reactivestreams.Publisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import se.magnus.api.core.product.Product;
import se.magnus.api.core.product.ProductService;
import se.magnus.microservices.core.product.persistence.ProductEntity;
import se.magnus.microservices.core.product.persistence.ProductRepository;
import se.magnus.util.exceptions.InvalidInputException;
import se.magnus.util.exceptions.NotFoundException;
import se.magnus.util.http.ServiceUtil;

import java.util.function.Supplier;

import static reactor.core.publisher.Mono.error;

@RestController
@Slf4j
public class ProductServiceImpl implements ProductService {
    private final Scheduler scheduler;
    private final ServiceUtil serviceUtil;

    private final ProductRepository repository;

    private final ProductMapper mapper;

    @Autowired
    public ProductServiceImpl(Scheduler scheduler,
                              ProductRepository repository,
                              ProductMapper mapper,
                              ServiceUtil serviceUtil) {
        this.scheduler = scheduler;
        this.serviceUtil = serviceUtil;
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Product createProduct(Product body) {
        if (body.getProductId() < 1) throw new InvalidInputException("Invalid productId: " + body.getProductId());

        try {
            ProductEntity entity = mapper.apiToEntity(body);
            ProductEntity newEntity = repository.save(entity);

            log.debug("create product. {}", body.getProductId());

            return mapper.entityToApi(newEntity);
        } catch (DataIntegrityViolationException e) {
            throw new InvalidInputException(("dup key. product ID: " + body.getProductId()));
        }
    }

    @Override
    public Mono<Product> getProduct(int productId) {
        if (productId < 1) throw new InvalidInputException("Invalid product Id: " + productId);

        return Mono.defer(() -> Mono.just(_getProduct(productId))).subscribeOn(scheduler);
    }

    protected Product _getProduct(int productId) {
        ProductEntity entity = repository.findByProductId(productId)
                .orElseThrow(() -> new NotFoundException("No product found for productId: " + productId));

        Product p = mapper.entityToApi(entity);
        p.setServiceAddress(serviceUtil.getServiceAddress());
        return p;
    }

    @Override
    public void deleteProduct(int productId) {
        log.debug("deleteProduct: tries to delete an entity with productId: {}", productId);
        //repository.findByProductId(productId).ifPresent(e -> repository.delete(e));
        repository.findByProductId(productId).ifPresent(repository::delete);
    }

}
