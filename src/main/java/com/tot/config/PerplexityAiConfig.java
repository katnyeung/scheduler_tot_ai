package com.tot.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class PerplexityAiConfig {

    @Value("${perplexity.api-key}")
    private String apiKey;

    @Value("${perplexity.base-url:https://api.perplexity.ai}")
    private String baseUrl;

    @Value("${perplexity.model:llama-3-sonar-small-chat}")
    private String model;

    @Value("${perplexity.temperature:0.7}")
    private double temperature;

    /**
     * Creates a custom OpenAiApi configured to use Perplexity's API
     * instead of OpenAI's API
     */
    @Bean
    @Primary
    public OpenAiApi perplexityApi() {
        RestClient.Builder builder = RestClient.builder()
                .baseUrl(baseUrl);

        WebClient.Builder webClientBuilder = WebClient.builder()
                .baseUrl(baseUrl);
        // Using the constructor directly as shown in the source code
        return new OpenAiApi(baseUrl, apiKey, builder, webClientBuilder);
    }

    /**
     * Creates a ChatModel using the Perplexity API
     */
    @Bean
    @Primary
    public OpenAiChatModel chatModel(OpenAiApi perplexityApi) {
        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .model(model)
                .temperature((double) temperature)
                .build();

        return new OpenAiChatModel(perplexityApi, options);
    }

    /**
     * Creates a ChatClient using the ChatModel
     */
    @Bean
    @Primary
    public ChatClient chatClient(OpenAiChatModel chatModel) {
        return ChatClient.builder(chatModel).build();
    }
}
