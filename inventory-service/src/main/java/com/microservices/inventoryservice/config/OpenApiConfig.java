package com.microservices.inventoryservice.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI (Swagger) documentation for Inventory Service.
 * Provides interactive API documentation at /swagger-ui.html
 * 
 * Services: Inventory management with Kafka event consumption and saga compensation.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI inventoryServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Inventory Service API")
                        .description("Inventory Management Service. " +
                                "Handles stock management, reservation, and allocation. " +
                                "Features: Kafka event consumption (saga step 2), " +
                                "compensation handling (release inventory on payment failure).")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Order Management Microservices")
                                .url("http://localhost:8084"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0.html")));
    }
}
