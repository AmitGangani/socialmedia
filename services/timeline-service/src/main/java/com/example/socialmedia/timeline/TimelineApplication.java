package com.example.socialmedia.timeline;

import java.net.http.HttpClient;
import java.time.Clock;
import java.time.Duration;
import java.util.UUID;
import java.util.function.Supplier;

import com.example.socialmedia.timeline.integration.FollowClient;
import com.example.socialmedia.timeline.integration.PostClient;
import com.github.f4b6a3.uuid.UuidCreator;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@SpringBootApplication
public class TimelineApplication {

    public static void main(String[] args) {
        SpringApplication.run(TimelineApplication.class, args);
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
    FollowClient followClient(
            @Qualifier("loadBalancedRestClientBuilder") RestClient.Builder builder,
            @Value("${clients.follow-service.base-url}") String followServiceBaseUrl) {
        return new FollowClient(builder, followServiceBaseUrl);
    }

    @Bean
    PostClient postClient(@Qualifier("loadBalancedRestClientBuilder") RestClient.Builder builder,
            @Value("${clients.post-service.base-url}") String postServiceBaseUrl,
            CircuitBreakerFactory<?, ?> circuitBreakerFactory) {
        return new PostClient(builder, postServiceBaseUrl, circuitBreakerFactory);
    }
}
