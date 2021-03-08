package se.magnus.microservices.core.review.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.cloud.stream.messaging.Sink;
import se.magnus.api.core.review.Review;
import se.magnus.api.core.review.ReviewService;
import se.magnus.api.event.Event;
import se.magnus.util.exceptions.EventProcessingException;

@EnableBinding(Sink.class)
@Slf4j
public class MessageProcessor {
    private final ReviewService reviewService;

    @Autowired
    public MessageProcessor(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @StreamListener(target = Sink.INPUT)
    public void process(Event<Integer, Review> event) {
        log.info("process message created at {}...", event.getEventCreatedAt());

        switch (event.getEventType()) {
            case CREATE:
                Review review = event.getData();
                log.info("create review with id : {} / {}", review.getProductId(), review.getReviewId());
                reviewService.createReview(review);
                break;

            case DELETE:
                int productId = event.getKey();
                log.info("delete review with id : {}", productId);
                reviewService.deleteReviews(productId);
                break;

            default:
                String errMsg = "incorrect event type: " + event.getEventType();
                log.warn(errMsg);
                throw new EventProcessingException(errMsg);
        }

        log.info("message processing done");
    }
}
