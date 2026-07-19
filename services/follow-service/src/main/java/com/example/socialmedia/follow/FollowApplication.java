package com.example.socialmedia.follow;

import java.net.http.HttpClient;
import java.time.Clock;
import java.time.Duration;
import java.util.UUID;
import java.util.function.Supplier;

import com.example.socialmedia.follow.integration.UserClient;
import com.github.f4b6a3.uuid.UuidCreator;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestClient;

@SpringBootApplication
@EnableScheduling
public class FollowApplication {

    public static void main(String[] args) {
        SpringApplication.run(FollowApplication.class, args);
    }

    @Bean
    Supplier<UUID> uuidV7Generator() {
        return UuidCreator::getTimeOrderedEpoch;
    }

    @Bean
    Clock systemClock() {
        return Clock.systemUTC();
    }

    @Bean
    JdkClientHttpRequestFactory restClientRequestFactory(
            @Value("${clients.connect-timeout}") Duration connectTimeout,
            @Value("${clients.read-timeout}") Duration readTimeout) {
        HttpClient httpClient = HttpClient.newBuilder().connectTimeout(connectTimeout).build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(readTimeout);
        return requestFactory;
    }

    @Bean
    @Primary
    RestClient.Builder defaultRestClientBuilder(JdkClientHttpRequestFactory requestFactory) {
        return RestClient.builder().requestFactory(requestFactory);
    }

    @Bean
    @LoadBalanced
    RestClient.Builder loadBalancedRestClientBuilder(JdkClientHttpRequestFactory requestFactory) {
        return RestClient.builder().requestFactory(requestFactory)
                .requestInterceptor((request, body, execution) -> {
                    String correlationId = MDC.get("correlationId");
                    if (correlationId != null) {
                        request.getHeaders().set("X-Correlation-Id", correlationId);
                    }
                    return execution.execute(request, body);
                });
    }

    @Bean
    UserClient userClient(@Qualifier("loadBalancedRestClientBuilder") RestClient.Builder builder,
            @Value("${clients.user-service.base-url}") String userServiceBaseUrl) {
        return new UserClient(builder, userServiceBaseUrl);
    }
}
