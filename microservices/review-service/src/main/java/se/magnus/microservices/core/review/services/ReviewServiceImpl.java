package se.magnus.microservices.core.review.services;

import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Scheduler;
import se.magnus.api.core.review.Review;
import se.magnus.api.core.review.ReviewService;
import se.magnus.microservices.core.review.persistence.ReviewEntity;
import se.magnus.microservices.core.review.persistence.ReviewRepository;
import se.magnus.util.exceptions.InvalidInputException;
import se.magnus.util.http.ServiceUtil;

import java.util.List;
import java.util.function.Supplier;

import static java.util.logging.Level.FINE;

@RestController
@Slf4j
public class ReviewServiceImpl implements ReviewService {
    private final Scheduler        scheduler;
    private final ReviewRepository repository;
    private final ReviewMapper     mapper;
    private final ServiceUtil      serviceUtil;

    @Autowired
    public ReviewServiceImpl(Scheduler scheduler,
                             ReviewRepository repository,
                             ReviewMapper mapper,
                             ServiceUtil serviceUtil) {
        this.scheduler = scheduler;
        this.repository = repository;
        this.mapper = mapper;
        this.serviceUtil = serviceUtil;
    }

    @Override 
    public Flux<Review> getReviews(int productId) {
        if (productId < 1) throw new InvalidInputException("Invalid product Id: " + productId);
        log.info("will get reviews. product id = {}", productId);

        return asyncFlux(() -> Flux.fromIterable(getByProductId(productId)).log(null, FINE));
    }

    private List<Review> getByProductId(int productId) {
        List<ReviewEntity> entityList = repository.findByProductId(productId);
        List<Review> list = mapper.entityListToApiList(entityList);
        list.forEach(e -> e.setServiceAddress(serviceUtil.getServiceAddress()));

        log.debug("get review : response size: {}", list.size());

        return list;
    }

    @Override
    public Review createReview(Review body) {
        if (body.getProductId() < 1) throw new InvalidInputException("Invalid productId: " + body.getProductId());

        try {
            ReviewEntity entity = mapper.apiToEntity(body);
            ReviewEntity newEntity = repository.save(entity);

            log.debug("create review. productid = {}, reviewid = {}", body.getProductId(), body.getReviewId());
            return mapper.entityToApi(newEntity);
        } catch(DataIntegrityViolationException e) {
            throw new InvalidInputException("dup key. product id : " + body.getProductId() + ", Review Id:" + body.getReviewId());
        }
    }

    @Override
    public void deleteReviews(int productId) {
        if (productId < 1) throw new InvalidInputException("Invalid productId: " + productId);
        log.debug("delete reviews. product id = {}", productId);
        repository.deleteAll(repository.findByProductId(productId));
    }

    private <T> Flux<T> asyncFlux(Supplier<Publisher<T>> publisherSupplier) {
        return Flux.defer(publisherSupplier).subscribeOn(scheduler);
    }
}