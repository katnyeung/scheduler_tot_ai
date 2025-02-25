package com.tot.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI experimentalOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Experimental New Era of Programming API")
                        .description("API endpoints for scheduling, managing TOT, executing actions, and refining decisions using Spring AI")
                        .version("1.0.0"));
    }
}