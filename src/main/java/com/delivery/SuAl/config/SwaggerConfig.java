package com.delivery.SuAl.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Your API")
                        .version("1.0"));
    }

    @Bean
    public OperationCustomizer customize() {
        return (operation, handlerMethod) -> {
            if (operation.getRequestBody() != null &&
                    operation.getRequestBody().getContent() != null &&
                    operation.getRequestBody().getContent().containsKey("multipart/form-data")) {

                io.swagger.v3.oas.models.media.Content content = operation.getRequestBody().getContent();
                io.swagger.v3.oas.models.media.MediaType mediaType = content.get("multipart/form-data");

                if (mediaType != null && mediaType.getSchema() != null) {
                    mediaType.getSchema().setType("object");
                }
            }
            return operation;
        };
    }
}
