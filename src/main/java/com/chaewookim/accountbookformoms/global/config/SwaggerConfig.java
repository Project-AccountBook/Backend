package com.chaewookim.accountbookformoms.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public GroupedOpenApi accountGroup() {
        return GroupedOpenApi.builder()
                .group("Account API")
                .pathsToMatch("/api/v1/account/**")
                .build();
    }

    @Bean
    public GroupedOpenApi userGroup() {
        return GroupedOpenApi.builder()
                .group("User API")
                .pathsToMatch(
                        "/api/v1/auth/**",
                        "/api/v1/users/**"
                )
                .build();
    }

    @Bean
    public GroupedOpenApi transactionGroup() {
        return GroupedOpenApi.builder()
                .group("Transaction API")
                .pathsToMatch(
                        "/api/v1/transactions/**"
                )
                .build();
    }

    @Bean
    public GroupedOpenApi fixedTransactionGroup() {
        return GroupedOpenApi.builder()
                .group("FixedTransaction API")
                .pathsToMatch(
                        "/api/v1/fixed-transactions/**"
                )
                .build();
    }

    @Bean
    public GroupedOpenApi categoryGroup() {
        return GroupedOpenApi.builder()
                .group("Category API")
                .pathsToMatch(
                        "/api/v1/categories/**"
                )
                .build();
    }

    @Bean
    public GroupedOpenApi allGroup() {
        return GroupedOpenApi.builder()
                .group("전체 API")
                .pathsToMatch("/**")
                .build();
    }

    @Bean
    public OpenAPI openAPI() {
        String jwtSchemeName = "bearerAuth";

        SecurityScheme securityScheme = new SecurityScheme()
                .name(jwtSchemeName)
                .type(SecurityScheme.Type.HTTP)   // HTTP 인증
                .scheme("bearer")                 // Bearer 토큰
                .bearerFormat("JWT")              // 표시용
                .in(SecurityScheme.In.HEADER);    // Authorization 헤더

        SecurityRequirement securityRequirement = new SecurityRequirement()
                .addList(jwtSchemeName);

        return new OpenAPI()
                .components(new Components()
                        .addSecuritySchemes(jwtSchemeName, securityScheme))
                .addSecurityItem(securityRequirement)
                .info(new Info()
                        .title("AccountBookForMoms API")
                        .version("v1"));
    }
}