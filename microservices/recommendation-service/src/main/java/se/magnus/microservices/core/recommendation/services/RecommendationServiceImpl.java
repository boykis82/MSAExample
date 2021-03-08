package se.magnus.microservices.core.recommendation.services;

import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Scheduler;
import se.magnus.api.core.recommendation.Recommendation;
import se.magnus.api.core.recommendation.RecommendationService;
import se.magnus.microservices.core.recommendation.persistence.RecommendationEntity;
import se.magnus.microservices.core.recommendation.persistence.RecommendationRepository;
import se.magnus.util.exceptions.InvalidInputException;
import se.magnus.util.http.ServiceUtil;

import java.util.List;
import java.util.function.Supplier;

import static java.util.logging.Level.FINE;

@RestController
@Slf4j
public class RecommendationServiceImpl implements RecommendationService {
    private final Scheduler scheduler;
    private final RecommendationRepository repository;
    private final RecommendationMapper mapper;

    private final ServiceUtil serviceUtil;

    @Autowired
    public RecommendationServiceImpl(Scheduler scheduler, RecommendationRepository repository,
                                    RecommendationMapper mapper,
                                     ServiceUtil serviceUtil) {
        this.scheduler = scheduler;
        this.repository = repository;
        this.mapper = mapper;
        this.serviceUtil = serviceUtil;
    }

    @Override 
    public Flux<Recommendation> getRecommendations(int productId) {
        if (productId < 1) throw new InvalidInputException("Invalid product Id: " + productId);

        log.info("will get recommendations. product id = {}", productId);

        return asyncFlux(() -> Flux.fromIterable(getByProductId(productId)).log(null, FINE));
    }


    private <T> Flux<T> asyncFlux(Supplier<Publisher<T>> publisherSupplier) {
        return Flux.defer(publisherSupplier).subscribeOn(scheduler);
    }

    private List<Recommendation> getByProductId(int productId) {
        List<RecommendationEntity> entityList = repository.findByProductId(productId);
        List<Recommendation> list = mapper.entityListToApiList(entityList);
        list.forEach(e -> e.setServiceAddress(serviceUtil.getServiceAddress()));

        log.debug("get recommendations : response size: {}", list.size());

        return list;
    }

    @Override
    public Recommendation createRecommendation(Recommendation body) {
        try {
            RecommendationEntity entity = mapper.apiToEntity(body);
            RecommendationEntity newEntity = repository.save(entity);

            log.debug("create recommendation. product id = {}, recommendation id = {}", body.getProductId(), body.getRecommendationId());
            return mapper.entityToApi(newEntity);
        } catch(DataIntegrityViolationException dive) {
            throw new InvalidInputException("dup key. product id: " + body.getProductId() + ", recommendation id: " + body.getRecommendationId());
        }
    }

    @Override
    public void deleteRecommendations(int productId) {
        log.debug("delete recommendation. product id = {}", productId);
        repository.deleteAll(repository.findByProductId(productId));
    }
}