package se.magnus.microservices.core.review.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;
import se.magnus.api.core.review.Review;
import se.magnus.api.core.review.ReviewService;
import se.magnus.util.exceptions.InvalidInputException;
import se.magnus.util.exceptions.NotFoundException;
import se.magnus.util.http.ServiceUtil;

import java.util.ArrayList;
import java.util.List;

@RestController
public class ReviewServiceImpl implements ReviewService {
    private static final Logger LOG = LoggerFactory.getLogger(ReviewServiceImpl.class);

    private final ServiceUtil serviceUtil;

    @Autowired
    public ReviewServiceImpl(ServiceUtil serviceUtil) {
        this.serviceUtil = serviceUtil;
    }

    @Override 
    public List<Review> getReviews(int productId) {
        if (productId < 1) throw new InvalidInputException("Invalid product Id: " + productId);
        if (productId == 213) {
            LOG.debug("no Reviews found for product Id: {}", productId);
            return new ArrayList<>();
        }

        List<Review> list = new ArrayList<>();
        list.add(new Review(productId, 1, "author 1", "subject 1", "content 1", serviceUtil.getServiceAddress()));
        list.add(new Review(productId, 2, "author 2", "subject 2", "content 2", serviceUtil.getServiceAddress()));
        list.add(new Review(productId, 3, "author 3", "subject 3", "content 3", serviceUtil.getServiceAddress()));

        LOG.debug("/Review response size: {}", list.size());

        return list;
    }   
}