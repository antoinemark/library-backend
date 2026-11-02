package com.anhanguera.library.config;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.handler.AbstractHandlerMapping;
import tools.jackson.databind.json.JsonMapper;

/**
 * Libera o acesso do frontend às rotas da API. As origens permitidas vêm da
 * propriedade app.cors.allowed-origins (lista separada por vírgula).
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    private final String[] allowedOrigins;

    public CorsConfig(@Value("${app.cors.allowed-origins:http://localhost:5173,http://localhost:4173}") String[] allowedOrigins) {
        this.allowedOrigins = allowedOrigins;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(allowedOrigins)
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders(HttpHeaders.LOCATION);
    }

    /**
     * Usa o {@link ApiErrorCorsProcessor} em todos os HandlerMappings do Spring MVC, para que a
     * rejeição de CORS (403) também responda no formato ApiError. Método static porque um
     * BeanPostProcessor precisa ser criado antes dos demais beans.
     */
    @Bean
    static BeanPostProcessor apiErrorCorsProcessorInstaller(ObjectProvider<JsonMapper> jsonMapper) {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessBeforeInitialization(Object bean, String beanName) {
                if (bean instanceof AbstractHandlerMapping handlerMapping) {
                    handlerMapping.setCorsProcessor(new ApiErrorCorsProcessor(jsonMapper));
                }
                return bean;
            }
        };
    }
}
