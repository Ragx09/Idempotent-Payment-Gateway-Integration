package dev.gabriel.idempotent_payment_gateway.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI metadata aligned with the idempotent payment gateway integration microservice scope.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI paymentGatewayOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Idempotent Payment Gateway Integration API")
                        .version("0.0.1-SNAPSHOT")
                        .description(
                                "Resilient payment microservice for idempotent payment gateway integration: "
                                        + "REST idempotency (Redis cache + distributed locks), "
                                        + "signed webhooks (HMAC-SHA256) with persisted retry handling, "
                                        + "Postgres ledger with pessimistic account locking; "
                                        + "load profile 4K+ concurrent requests (k6)."));
    }
}
