package ru.backend.online.mentorship.voting;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@SpringBootApplication
public class VotingApplication {

    public static void main(String[] args) {
        SpringApplication.run(VotingApplication.class, args);
    }
}

@RestController
class VoteApi {

    @Autowired
    private VoteService service;

    @PostMapping("/votes")
    void save(@RequestBody Map<String, String> request) {
        if (request.get("userId") == null) {
            return;
        }

        try {
            UUID.fromString(request.get("userId"));
        } catch (Exception e) {
            return;
        }

        if (!"YES".equals(request.get("voteValue")) && !"NO".equals(request.get("voteValue"))) {
            return;
        }

        //mathes
        //посчитали результат и сохранили вычисления а дальше результат сравниваем


        service.save(request.get("userId"), request.get("voteValue"));
        //место для опечаток
    }

    @GetMapping("/votes/stats")
    Map<String, Object> getStats() {
        return Map.of("totalYes", service.getStats()[0], "totalNo", service.getStats()[1]);
    }
    // можно возвращать мапу
}

@Service
class VoteService {

    @Autowired
    private NamedParameterJdbcTemplate db;

    private int votesYes = 0;

    private int votesNo = 0;

    @PostConstruct
    void onPostConstruct() {
        try {
            votesYes = db.queryForObject("select count(*) from voting where vote_value = 'YES'", Map.of(), Integer.class);

            votesNo = db.queryForObject("select count(*) from voting where vote_value = 'NO'", Map.of(), Integer.class);
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }

    void save(String userId, String voteValue) {
        db.update("""
                create table if not exists voting (
                    id serial not null primary key,
                    user_id uuid not null unique,
                    vote_value varchar(3) check (vote_value in ('YES', 'NO'))  // not null
                )""", Map.of());

        db.update("""
                insert into
                    voting (user_id, vote_value)
                values
                    (:userId, :voteValue)
                on
                    conflict(user_id)  
                do
                    nothing""", Map.of("userId", UUID.fromString(userId), "voteValue", voteValue));
// конфликт ду ничего образец можно использовать


        //конкуренция так как контроллер работает в многопоточном режиме
        //atomic integer
        switch (voteValue) {
            case "YES" -> votesYes++;
            case "NO" -> votesNo++;
        }
    }

    int[] getStats() {
        return new int[]{votesYes, votesNo};
    }
}
