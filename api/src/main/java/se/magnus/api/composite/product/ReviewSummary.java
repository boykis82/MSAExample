package se.magnus.api.composite.product;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class ReviewSummary {

  private int reviewId;
  private String author;
  private String subject;

}
