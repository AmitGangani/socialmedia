package com.example.socialmedia.gateway;

import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.UUID;
import java.util.regex.Pattern;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

    public static final String HEADER_NAME = "X-Correlation-Id";
    public static final String REQUEST_ATTRIBUTE = CorrelationIdFilter.class.getName() + ".value";
    private static final Pattern SAFE_VALUE = Pattern.compile("[A-Za-z0-9._:-]{1,128}");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String correlationId = correlationId(request.getHeader(HEADER_NAME));
        HttpServletRequest wrappedRequest = new CorrelationRequest(request, correlationId);
        wrappedRequest.setAttribute(REQUEST_ATTRIBUTE, correlationId);
        response.setHeader(HEADER_NAME, correlationId);

        MDC.put("correlationId", correlationId);
        MDC.put("operation", request.getMethod() + " " + request.getRequestURI());
        try {
            filterChain.doFilter(wrappedRequest, response);
        }
        finally {
            MDC.remove("operation");
            MDC.remove("correlationId");
        }
    }

    private static String correlationId(String candidate) {
        if (candidate != null && SAFE_VALUE.matcher(candidate.trim()).matches()) {
            return candidate.trim();
        }
        return UUID.randomUUID().toString();
    }

    private static final class CorrelationRequest extends HttpServletRequestWrapper {

        private final String correlationId;

        private CorrelationRequest(HttpServletRequest request, String correlationId) {
            super(request);
            this.correlationId = correlationId;
        }

        @Override
        public String getHeader(String name) {
            return HEADER_NAME.equalsIgnoreCase(name) ? correlationId : super.getHeader(name);
        }

        @Override
        public Enumeration<String> getHeaders(String name) {
            return HEADER_NAME.equalsIgnoreCase(name)
                    ? Collections.enumeration(Collections.singleton(correlationId))
                    : super.getHeaders(name);
        }
    }
}
