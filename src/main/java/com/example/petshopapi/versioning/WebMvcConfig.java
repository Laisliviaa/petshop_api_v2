package com.example.petshopapi.versioning;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final ApiVersionInterceptor apiVersionInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(apiVersionInterceptor)
                .addPathPatterns(
                    "/api/v1/clientes/**",
                    "/api/v1/pets/**",
                    "/api/v1/agendamentos/**",
                    "/api/v1/servicos/**",
                    "/api/v1/unidades/**",
                    "/api/v1/gerentes/**"
                );
    }
}
