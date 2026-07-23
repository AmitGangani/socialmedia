package com.example.socialmedia.notification;

import java.time.Clock;
import java.util.UUID;
import java.util.function.Supplier;

import com.github.f4b6a3.uuid.UuidCreator;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class NotificationApplication {

    public static void main(String[] args) {
        SpringApplication.run(NotificationApplication.class, args);
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
