package se.magnus.microservices.core.product.persistence;

import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.Optional;

public interface ProductRepository extends PagingAndSortingRepository<ProductEntity, Integer> {
    Optional<ProductEntity> findByProductId(int productId);
}
