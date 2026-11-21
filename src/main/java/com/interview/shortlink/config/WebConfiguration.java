package com.interview.shortlink.config;

import com.interview.shortlink.web.CreateRateLimitInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfiguration implements WebMvcConfigurer {

    private final CreateRateLimitInterceptor rateLimitInterceptor;

    public WebConfiguration(CreateRateLimitInterceptor rateLimitInterceptor) {
        this.rateLimitInterceptor = rateLimitInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(rateLimitInterceptor)
                .addPathPatterns("/api/v1/links")
                .excludePathPatterns("/api-docs/**", "/swagger-ui/**");
    }
}
