package com.gymcrm.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
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
    private static final String LOGOUT_PATH = "/auth/logout";

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
                                        .description("Token returned by POST /auth/login or by registration")));
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
            // Logout is handled by the Spring Security filter chain, so springdoc cannot discover it.
            openApi.getPaths().addPathItem(LOGOUT_PATH, new PathItem().post(new Operation()
                    .addTagsItem("Authentication")
                    .summary("Logout")
                    .description("Revokes the Bearer token sent with the request; it can no longer be used.")
                    .security(List.of(new SecurityRequirement().addList(BEARER_AUTH)))
                    .responses(new ApiResponses()
                            .addApiResponse("200", new ApiResponse().description("Logged out; token revoked"))
                            .addApiResponse("401", new ApiResponse().description("Missing/invalid token")))));
        };
    }

    private boolean isPublic(String path, PathItem.HttpMethod method) {
        if (method != PathItem.HttpMethod.POST) {
            return false;
        }
        return path.equals("/auth/login") || path.equals("/trainees") || path.equals("/trainers");
    }
}
