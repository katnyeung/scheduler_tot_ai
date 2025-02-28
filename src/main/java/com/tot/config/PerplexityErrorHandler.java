package com.tot.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResponseErrorHandler;

import java.io.IOException;

@Component
public class PerplexityErrorHandler implements ResponseErrorHandler {
    private static final Logger log = LoggerFactory.getLogger(PerplexityErrorHandler.class);

    @Override
    public boolean hasError(ClientHttpResponse response) throws IOException {
        return response.getStatusCode().isError();
    }

    @Override
    public void handleError(ClientHttpResponse response) throws IOException {
        if (response.getHeaders().getContentType() != null &&
                response.getHeaders().getContentType().includes(MediaType.TEXT_HTML)) {
            log.error("Received HTML error response from Perplexity API: {}", response.getStatusCode());
            throw new RuntimeException("Perplexity API returned HTML error page with status: " + response.getStatusCode());
        }

        // Handle JSON error responses
        String responseBody = new String(response.getBody().readAllBytes());
        log.error("Error response from Perplexity API: {} - {}", response.getStatusCode(), responseBody);
        throw new RuntimeException("Perplexity API error: " + response.getStatusCode() + " - " + responseBody);
    }
}