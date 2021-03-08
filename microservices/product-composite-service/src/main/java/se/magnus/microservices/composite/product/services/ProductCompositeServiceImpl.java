package se.magnus.microservices.composite.product.services;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import se.magnus.api.composite.product.*;
import se.magnus.api.core.product.Product;
import se.magnus.api.core.recommendation.Recommendation;
import se.magnus.api.core.review.Review;
import se.magnus.util.http.ServiceUtil;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@Slf4j
public class ProductCompositeServiceImpl implements ProductCompositeService {

    private final ServiceUtil serviceUtil;
    private ProductCompositeIntegration integration;

    @Autowired
    public ProductCompositeServiceImpl(ServiceUtil serviceUtil, ProductCompositeIntegration integration) {
        this.serviceUtil = serviceUtil;
        this.integration = integration;
    }

    @Override
    public void createCompositeProduct(ProductAggregate body) {
        try {
            log.debug("create composite product. productId : {}", body.getProductId());

            Product product = new Product(body.getProductId(),
                    body.getName(), body.getWeight(), null);
            integration.createProduct(product);

            if (body.getRecommendations() != null) {
                body.getRecommendations().forEach(r -> {
                    Recommendation recommendation = new Recommendation(
                            body.getProductId(),
                            r.getRecommendationId(),
                            r.getAuthor(),
                            r.getRate(),
                            r.getContent(),
                            null
                            );
                    integration.createRecommendation(recommendation);
                });
            }

            if (body.getReviews() != null) {
                body.getReviews().forEach(r -> {
                    Review review = new Review(
                            body.getProductId(),
                            r.getReviewId(),
                            r.getAuthor(),
                            r.getSubject(),
                            r.getContent(),
                            null);
                    integration.createReview(review);
                });
            }
        } catch (RuntimeException e) {
            log.warn("create composite product failed", e);
            throw e;
        }
    }

    @Override
    public Mono<ProductAggregate> getCompositeProduct(int productId) {
        return Mono.zip(values -> createProductAggregate((Product)values[0], (List<Recommendation>)values[1], (List<Review>)values[2], serviceUtil.getServiceAddress()),
                    integration.getProduct(productId),
                    integration.getRecommendations(productId).collectList(),
                    integration.getReviews(productId).collectList())
                .doOnError(e -> log.warn("getcompositeproduct failed: {}", e.toString()))
                .log();
    }

    @Override
    public void deleteCompositeProduct(int productId) {
        try {
            log.debug("delete composite product started. product id = {}", productId);
            integration.deleteProduct(productId);
            integration.deleteRecommendations(productId);
            integration.deleteReviews(productId);
            log.debug("delete composite product completed. product id = {}", productId);
        } catch(RuntimeException ex) {
            log.warn("deleteCompositeProduct failed: {}", ex.toString());
            throw ex;
        }
    }

    private ProductAggregate createProductAggregate(
            Product product, List<Recommendation> recommendations, List<Review> reviews, String serviceAddress) {
        int productId = product.getProductId();
        String name = product.getName();
        int weight = product.getWeight();

        List<RecommendationSummary> recommendationSummaries = (recommendations == null) ? null :
                recommendations.stream()
                .map(r -> new RecommendationSummary(r.getRecommendationId(), r.getAuthor(), r.getRate(), r.getContent()))
                .collect(Collectors.toList());

        List<ReviewSummary> reviewSummaries = (reviews == null) ? null :
                reviews.stream()
                        .map(r -> new ReviewSummary(r.getReviewId(), r.getAuthor(), r.getSubject(), r.getContent()))
                        .collect(Collectors.toList());

        String productAddress = product.getServiceAddress();
        String reviewAddress =
                (reviews != null && reviews.size() > 0) ? reviews.get(0).getServiceAddress() : "";
        String recommendationAddress =
                (recommendations != null && recommendations.size() > 0) ? recommendations.get(0).getServiceAddress() : "";
        ServiceAddresses serviceAddresses = new ServiceAddresses(serviceAddress, productAddress, reviewAddress, recommendationAddress);

        return new ProductAggregate(productId, name, weight, recommendationSummaries, reviewSummaries, serviceAddresses);
    }
}
