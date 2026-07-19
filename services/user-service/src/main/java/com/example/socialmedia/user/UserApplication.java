package com.example.socialmedia.user;

import java.net.http.HttpClient;
import java.time.Duration;

import com.example.socialmedia.user.integration.ProfileCountClient;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.client.RestClient;

@SpringBootApplication
public class UserApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserApplication.class, args);
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
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
        return RestClient.builder()
                .requestFactory(requestFactory)
                .requestInterceptor((request, body, execution) -> {
                    String correlationId = MDC.get("correlationId");
                    if (correlationId != null) {
                        request.getHeaders().set("X-Correlation-Id", correlationId);
                    }
                    return execution.execute(request, body);
                });
    }

    @Bean
    ProfileCountClient profileCountClient(
            @Qualifier("loadBalancedRestClientBuilder") RestClient.Builder builder,
            @Value("${clients.follow-service.base-url}") String followServiceBaseUrl,
            @Value("${clients.post-service.base-url}") String postServiceBaseUrl) {
        return new ProfileCountClient(builder, followServiceBaseUrl, postServiceBaseUrl);
    }
}
