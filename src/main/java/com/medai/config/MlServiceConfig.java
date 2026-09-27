package com.medai.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class MlServiceConfig {

    @Value("${app.ml-service.base-url}")
    private String mlServiceBaseUrl;

    @Bean(name = "mlServiceWebClient")
    public WebClient mlServiceWebClient() {
        return WebClient.builder()
                .baseUrl(mlServiceBaseUrl)
                .codecs(configurer -> configurer.defaultCodecs()
                        .maxInMemorySize(50 * 1024 * 1024)) // 50MB for image uploads
                .build();
    }

    public String getMlServiceBaseUrl() {
        return mlServiceBaseUrl;
    }
}
