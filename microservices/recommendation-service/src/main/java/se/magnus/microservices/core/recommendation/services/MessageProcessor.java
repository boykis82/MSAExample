package se.magnus.microservices.core.recommendation.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.cloud.stream.messaging.Sink;
import se.magnus.api.core.recommendation.Recommendation;
import se.magnus.api.core.recommendation.RecommendationService;
import se.magnus.api.event.Event;
import se.magnus.util.exceptions.EventProcessingException;

@EnableBinding(Sink.class)
@Slf4j
public class MessageProcessor {
    private final RecommendationService recommendationService;

    @Autowired
    public MessageProcessor(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    @StreamListener(target = Sink.INPUT)
    public void process(Event<Integer, Recommendation> event) {
        log.info("process message created at {}...", event.getEventCreatedAt());

        switch (event.getEventType()) {
            case CREATE:
                Recommendation recommendation = event.getData();
                log.info("create recommedation with id : {} / {}", recommendation.getProductId(), recommendation.getRecommendationId());
                recommendationService.createRecommendation(recommendation);
                break;

            case DELETE:
                int productId = event.getKey();
                log.info("delete recommendation with id : {}", productId);
                recommendationService.deleteRecommendations(productId);
                break;

            default:
                String errMsg = "incorrect event type: " + event.getEventType();
                log.warn(errMsg);
                throw new EventProcessingException(errMsg);
        }

        log.info("message processing done");
    }
}
