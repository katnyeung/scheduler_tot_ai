package com.tot.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class PerplexityService {
    private final RestClient restClient;
    private final String defaultModel;
    private final ObjectMapper objectMapper;
    private static final Logger log = LoggerFactory.getLogger(PerplexityService.class);

    public PerplexityService(RestClient restClient, String defaultModel) {
        this.restClient = restClient;
        this.defaultModel = defaultModel;
        this.objectMapper = new ObjectMapper();
        log.info("PerplexityService initialized with model: {}", defaultModel);
    }

    /**
     * Generate a completion from Perplexity API
     * @param prompt The user prompt
     * @return The generated completion text
     */
    public String generateCompletion(String prompt) {
        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", defaultModel);
            requestBody.put("messages", List.of(
                    Map.of("role", "user", "content", prompt)
            ));

            log.debug("Sending request to Perplexity API with model: {}", defaultModel);
            String responseJson = restClient.post()
                    .uri("/chat/completions")
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);

            // Parse the response
            JsonNode rootNode = objectMapper.readTree(responseJson);
            String content = rootNode.path("choices").path(0).path("message").path("content").asText();
            log.debug("Received response from Perplexity API: {}", content);
            return content;
        } catch (Exception e) {
            log.error("Error calling Perplexity API", e);
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Generate a completion with system and user messages
     * @param systemPrompt The system message content
     * @param userPrompt The user message content
     * @return The generated completion text
     */
    public String generateCompletionWithSystem(String systemPrompt, String userPrompt) {
        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", defaultModel);

            List<Map<String, String>> messages = new ArrayList<>();
            messages.add(Map.of("role", "system", "content", systemPrompt));
            messages.add(Map.of("role", "user", "content", userPrompt));
            requestBody.put("messages", messages);

            log.debug("Sending request to Perplexity API with system prompt");
            String responseJson = restClient.post()
                    .uri("/chat/completions")
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);

            // Parse the response
            JsonNode rootNode = objectMapper.readTree(responseJson);
            String content = rootNode.path("choices").path(0).path("message").path("content").asText();
            log.debug("Received response from Perplexity API: {}", content);
            return content;
        } catch (Exception e) {
            log.error("Error calling Perplexity API with system prompt", e);
            return "Error: " + e.getMessage();
        }
    }
}