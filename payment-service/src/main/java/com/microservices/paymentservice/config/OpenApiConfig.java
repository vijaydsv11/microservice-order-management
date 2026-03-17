package com.microservices.paymentservice.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI (Swagger) documentation for Payment Service.
 * Provides interactive API documentation at /swagger-ui.html
 * 
 * Services: Payment processing and transaction management via saga pattern.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI paymentServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Payment Service API")
                        .description("Payment Processing Service. " +
                                "Handles payment authorization and processing (saga step 3). " +
                                "Features: @Transactional payment processing, idempotency guards, " +
                                "pluggable payment gateway integration.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Order Management Microservices")
                                .url("http://localhost:8086"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0.html")));
    }
}
