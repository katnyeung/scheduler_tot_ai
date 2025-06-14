package com.tot.config;

import com.tot.service.PerplexityService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class PerplexityConfig {

    @Value("${perplexity.api-key}")
    private String apiKey;

    @Value("${perplexity.base-url:https://api.perplexity.ai}")
    private String baseUrl;

    @Value("${perplexity.model:llama-3.1-sonar-small-128k-online}")
    private String model;

    @Bean
    public ClientHttpRequestFactory requestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(30000); // 30 seconds connection timeout
        factory.setReadTimeout(120000);   // 2 minutes read timeout
        return factory;
    }

    @Bean
    public RestClient perplexityRestClient(ClientHttpRequestFactory requestFactory) {
        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .build();
    }

    @Bean
    public PerplexityService perplexityService(RestClient perplexityRestClient) {
        return new PerplexityService(perplexityRestClient, model);
    }
}