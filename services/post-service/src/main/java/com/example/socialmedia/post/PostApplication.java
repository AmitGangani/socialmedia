package com.example.socialmedia.post;

import java.time.Clock;
import java.util.UUID;
import java.util.function.Supplier;

import com.github.f4b6a3.uuid.UuidCreator;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PostApplication {

    public static void main(String[] args) {
        SpringApplication.run(PostApplication.class, args);
    }

    @Bean
    Supplier<UUID> uuidV7Generator() {
        return UuidCreator::getTimeOrderedEpoch;
    }

    @Bean
    Clock systemClock() {
        return Clock.systemUTC();
    }
}
