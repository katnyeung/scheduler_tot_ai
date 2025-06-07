package com.tot.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI totOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Tree of Thought Scheduling System API")
                        .description("API endpoints for Tree of Thought processing, scheduling, action execution, and logging")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("ToT System")));
    }
}