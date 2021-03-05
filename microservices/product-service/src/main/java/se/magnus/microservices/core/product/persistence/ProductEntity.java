package se.magnus.microservices.core.product.persistence;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;

@Entity
@Table(
        name = "products",
        indexes = {
                @Index(name = "product_unique_idx", unique = true, columnList = "productId")
        })
@Getter
@Setter
@NoArgsConstructor
public class ProductEntity {

    @Id
    @GeneratedValue
    private int id;

    @Version
    private int version;

    private int productId;

    private String name;
    private int weight;

    public ProductEntity(int productId, String name, int weight) {
        this.productId = productId;
        this.name = name;
        this.weight = weight;
    }
}
