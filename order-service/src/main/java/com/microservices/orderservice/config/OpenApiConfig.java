package com.microservices.orderservice.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI (Swagger) documentation for Order Service.
 * Provides interactive API documentation at /swagger-ui.html
 * 
 * Services: Order management with distributed saga, payment processing, and
 * inventory reservation.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI orderServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Order Service API")
                        .description("Order Management Service with distributed saga pattern. " +
                                "Handles order creation, payment processing, and inventory reservation. " +
                                "Features: Feign client to User Service, Circuit Breaker/Retry resilience, " +
                                "Event-driven saga pattern via Kafka.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Order Management Microservices")
                                .url("http://localhost:8083"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0.html")));
    }
}
