package com.microservices.apigateway.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI (Swagger) documentation for API Gateway.
 * Provides centralized API documentation.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI apiGatewayOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Order Management Microservices API Gateway")
                        .description("Central gateway routing requests to all microservices: " +
                                "Auth, User, Order, Inventory, and Payment services")
                        .version("1.0.0"));
    }
}
