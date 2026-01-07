package com.example.cw1.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class AppConfig {


    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }


    @Bean
    @Qualifier("ilpBaseUrl")
    public String ilpBaseUrl() {

        String env = System.getenv("ILP_ENDPOINT");

        if (env != null && !env.isBlank()) {
            if (!env.endsWith("/")) {
                env = env + "/";
            }
            System.out.println(">>> Using environment variable ILP_ENDPOINT = " + env);
            return env;
        }

        String defaultUrl = "https://ilp-rest-2025-bvh6e9hschfagrgy.ukwest-01.azurewebsites.net/";
        System.out.println(">>> ILP_ENDPOINT not set, using default = " + defaultUrl);
        return defaultUrl;
    }
}
