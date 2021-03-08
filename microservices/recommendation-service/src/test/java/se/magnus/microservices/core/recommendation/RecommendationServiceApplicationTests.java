package se.magnus.microservices.core.recommendation;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.stream.messaging.Sink;
import org.springframework.http.HttpStatus;
import org.springframework.integration.channel.AbstractMessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.reactive.server.WebTestClient;
import se.magnus.api.core.recommendation.Recommendation;
import se.magnus.api.event.Event;
import se.magnus.microservices.core.recommendation.persistence.RecommendationRepository;
import se.magnus.util.exceptions.InvalidInputException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.HttpStatus.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static reactor.core.publisher.Mono.just;
import static se.magnus.api.event.Event.Type.CREATE;
import static se.magnus.api.event.Event.Type.DELETE;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = RANDOM_PORT, properties = {
		"spring.datasource.url=jdbc:h2:mem:recommendation-db"})
public class RecommendationServiceApplicationTests {

	@Autowired
	private WebTestClient client;

	@Autowired
	private RecommendationRepository repository;


	@Autowired
	private Sink channels;

	private AbstractMessageChannel input = null;

	@Before
	public void setupDb() {
		input = (AbstractMessageChannel) channels.input();
		repository.deleteAll();
	}

	@Test
	public void getRecommendationsByProductId() {

		int productId = 1;

		sendCreateRecommendationEvent(productId, 1);
		sendCreateRecommendationEvent(productId, 2);
		sendCreateRecommendationEvent(productId, 3);

		assertEquals(3, repository.findByProductId(productId).size());

		getAndVerifyRecommendationsByProductId(productId, OK)
				.jsonPath("$.length()").isEqualTo(3)
				.jsonPath("$[2].productId").isEqualTo(productId)
				.jsonPath("$[2].recommendationId").isEqualTo(3);
	}

	@Test
	public void duplicateError() {

		int productId = 1;
		int recommendationId = 1;

		sendCreateRecommendationEvent(productId, recommendationId);

		assertEquals(1, repository.count());

		try {
			sendCreateRecommendationEvent(productId, recommendationId);
			fail("expected a messageing excepstion here!");
		} catch(MessagingException me) {
			if (me.getCause() instanceof InvalidInputException) {
				InvalidInputException iie = (InvalidInputException)me.getCause();

			} else {
				fail("expected a invalid input exception as the root cause!");
			}
		}
		assertEquals(1, repository.count());
	}

	@Test
	public void deleteRecommendations() {

		int productId = 1;
		int recommendationId = 1;

		sendCreateRecommendationEvent(productId, recommendationId);
		assertEquals(1, repository.findByProductId(productId).size());

		sendDeleteRecommendationEvent(productId);
		assertEquals(0, repository.findByProductId(productId).size());

		sendDeleteRecommendationEvent(productId);
	}

	@Test
	public void getRecommendationsMissingParameter() {

		getAndVerifyRecommendationsByProductId("", BAD_REQUEST)
				.jsonPath("$.path").isEqualTo("/recommendation");
	}

	@Test
	public void getRecommendationsInvalidParameter() {

		getAndVerifyRecommendationsByProductId("?productId=no-integer", BAD_REQUEST)
				.jsonPath("$.path").isEqualTo("/recommendation");
	}

	@Test
	public void getRecommendationsNotFound() {

		getAndVerifyRecommendationsByProductId("?productId=113", OK)
				.jsonPath("$.length()").isEqualTo(0);
	}

	@Test
	public void getRecommendationsInvalidParameterNegativeValue() {

		int productIdInvalid = -1;

		getAndVerifyRecommendationsByProductId("?productId=" + productIdInvalid, UNPROCESSABLE_ENTITY)
				.jsonPath("$.path").isEqualTo("/recommendation");
	}

	private WebTestClient.BodyContentSpec getAndVerifyRecommendationsByProductId(int productId, HttpStatus expectedStatus) {
		return getAndVerifyRecommendationsByProductId("?productId=" + productId, expectedStatus);
	}

	private WebTestClient.BodyContentSpec getAndVerifyRecommendationsByProductId(String productIdQuery, HttpStatus expectedStatus) {
		return client.get()
				.uri("/recommendation" + productIdQuery)
				.accept(APPLICATION_JSON)
				.exchange()
				.expectStatus().isEqualTo(expectedStatus)
				.expectHeader().contentType(APPLICATION_JSON)
				.expectBody();
	}

	private WebTestClient.BodyContentSpec postAndVerifyRecommendation(int productId, int recommendationId, HttpStatus expectedStatus) {
		Recommendation recommendation = new Recommendation(productId, recommendationId, "Author " + recommendationId, recommendationId, "Content " + recommendationId, "SA");
		return client.post()
				.uri("/recommendation")
				.body(just(recommendation), Recommendation.class)
				.accept(APPLICATION_JSON)
				.exchange()
				.expectStatus().isEqualTo(expectedStatus)
				.expectHeader().contentType(APPLICATION_JSON)
				.expectBody();
	}

	private WebTestClient.BodyContentSpec deleteAndVerifyRecommendationsByProductId(int productId, HttpStatus expectedStatus) {
		return client.delete()
				.uri("/recommendation?productId=" + productId)
				.accept(APPLICATION_JSON)
				.exchange()
				.expectStatus().isEqualTo(expectedStatus)
				.expectBody();
	}

	private void sendCreateRecommendationEvent(int productId, int recommendationId) {
		Recommendation review = new Recommendation(productId, recommendationId, "Author " + recommendationId, 1, "Content " + recommendationId, "SA");
		Event<Integer, Recommendation> event = new Event(CREATE, recommendationId, review);
		input.send(new GenericMessage<>(event));
	}

	private void sendDeleteRecommendationEvent(int productId) {
		Event<Integer, Recommendation> event = new Event(DELETE, productId, null);
		input.send(new GenericMessage<>(event));
	}
}
