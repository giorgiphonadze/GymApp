package com.gymcrm.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;

@Configuration
public class OpenApiConfig {
    public static final String BEARER_AUTH = "bearerAuth";

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH))
                .components(new Components()
                        .addSecuritySchemes(BEARER_AUTH,
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Token returned by POST /auth/login")));
    }

    @Bean
    public OpenApiCustomizer securityCustomizer() {
        return openApi -> {
            if (openApi.getPaths() == null) {
                return;
            }
            for (Map.Entry<String, PathItem> entry : openApi.getPaths().entrySet()) {
                String path = entry.getKey();
                PathItem item = entry.getValue();
                for (Map.Entry<PathItem.HttpMethod, Operation> op : item.readOperationsMap().entrySet()) {
                    Operation operation = op.getValue();
                    if (operation.getParameters() != null) {
                        operation.getParameters().removeIf(p -> "header".equals(p.getIn())
                                && "Authorization".equalsIgnoreCase(p.getName()));
                    }
                    if (isPublic(path, op.getKey())) {
                        operation.setSecurity(List.of());
                    } else {
                        operation.setSecurity(List.of(new SecurityRequirement().addList(BEARER_AUTH)));
                    }
                }
            }
        };
    }

    private boolean isPublic(String path, PathItem.HttpMethod method) {
        if (path.startsWith("/auth/")) {
            return true;
        }
        return method == PathItem.HttpMethod.POST && (path.equals("/trainees") || path.equals("/trainers"));
    }
}
