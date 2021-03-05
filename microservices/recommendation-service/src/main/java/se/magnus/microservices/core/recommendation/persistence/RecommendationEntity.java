package se.magnus.microservices.core.recommendation.persistence;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;

@Entity
@Table(
        name = "recommendations",
        indexes = {
                @Index(name = "recommendations_unique_idx",
                        unique = true,
                        columnList = "productId,recommendationId"
                )
        })
@Getter
@Setter
@NoArgsConstructor
public class RecommendationEntity {
    @Id
    @GeneratedValue
    private int id;

    @Version
    private int version;

    private int productId;
    private int recommendationId;
    private String author;
    private int rating;
    private String content;

    public RecommendationEntity(int productId, int recommendationId, String author, int rating, String content) {
        this.productId = productId;
        this.recommendationId = recommendationId;
        this.author = author;
        this.rating = rating;
        this.content = content;
    }

}
