package ru.backend.online.mentorship.voting;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class VoteApiTest {

    @LocalServerPort
    private int port;

    @Autowired
    private NamedParameterJdbcTemplate db;

    @Container
    private static PostgreSQLContainer<?> postgresContainer = new PostgreSQLContainer<>("postgres:9.5")
            .withDatabaseName("voting")
            .withUsername("voting")
            .withPassword("voting");

    @DynamicPropertySource
    static void updateDatasourceProperties(final DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgresContainer::getJdbcUrl);
        registry.add("spring.datasource.username", postgresContainer::getUsername);
        registry.add("spring.datasource.password", postgresContainer::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> postgresContainer.getDriverClassName());
    }

    @Test
    void saveRandomVotes() {
        var httpClient = new RestTemplateBuilder()
                .rootUri("http://127.0.0.1:" + port)
                .build();

        var initialVotesStatsAsJson = httpClient.getForObject("/votes/stats", JsonNode.class);
        var initialVotesYes = initialVotesStatsAsJson.get("totalYes").asInt();
        var initialVotesNo = initialVotesStatsAsJson.get("totalNo").asInt();

        var random = ThreadLocalRandom.current();

        var newVotesYes = random.nextInt(10) + 1; // at lease one vote
        var newVotesNo = random.nextInt(10) + 1; // at lease one vote

        var y = newVotesYes;
        var n = newVotesNo;

        var vote = (Consumer<String>) voteValue -> {
            var request = new ObjectMapper()
                    .createObjectNode()
                    .put("userId", UUID.randomUUID().toString())
                    .put("voteValue", voteValue);

            httpClient.postForObject("/votes", request, JsonNode.class);
        };

        while (y > 0 && n > 0) {
            var voteValue = random.nextBoolean() ? "YES" : "NO";

            vote.accept(voteValue);

            switch (voteValue) {
                case "YES" -> y--;
                case "NO" -> n--;
            }
        }

        while (y-- > 0) {
            vote.accept("YES");
        }

        while (n-- > 0) {
            vote.accept("NO");
        }

        var currentVotesStatsAsJson = httpClient.getForObject("/votes/stats", JsonNode.class);
        var currentVotesYes = currentVotesStatsAsJson.get("totalYes").asInt();
        var currentVotesNo = currentVotesStatsAsJson.get("totalNo").asInt();

        assertEquals(initialVotesYes + newVotesYes, currentVotesYes);
        assertEquals(initialVotesNo + newVotesNo, currentVotesNo);
    }
}
