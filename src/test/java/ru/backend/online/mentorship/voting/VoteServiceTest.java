package ru.backend.online.mentorship.voting;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
class VoteServiceTest {

    @Mock
    private NamedParameterJdbcTemplate db;

    @InjectMocks
    private VoteService service;

    @Test
    void saveYesVoteOnly() {
        service.save(UUID.randomUUID().toString(), "YES");

        assertEquals(1, service.getStats()[0]);
    }

    @Test
    void saveNoVoteOnly() {
        service.save(UUID.randomUUID().toString(), "NO");

        assertEquals(1, service.getStats()[1]);
    }

    @Test
    void saveRandomVotes() {
        var random = ThreadLocalRandom.current();

        var expectedVotesYes = random.nextInt(10) + 1; // at lease one vote
        var expectedVotesNo = random.nextInt(10) + 1; // at lease one vote

        var y = expectedVotesYes;
        var n = expectedVotesNo;

        var vote = (Consumer<String>) voteValue -> service.save(UUID.randomUUID().toString(), voteValue);

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

        assertArrayEquals(new int[]{expectedVotesYes, expectedVotesNo}, service.getStats());
    }
}