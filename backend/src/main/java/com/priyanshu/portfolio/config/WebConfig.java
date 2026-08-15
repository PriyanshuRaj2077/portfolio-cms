package com.priyanshu.portfolio.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.*;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Serve public static frontend from ../frontend/ or classpath
        registry.addResourceHandler("/**")
                .addResourceLocations("file:../frontend/", "classpath:/static/")
                .resourceChain(true)
                .addResolver(new PathResourceResolver());

        // Serve admin static frontend under /admin/**
        registry.addResourceHandler("/admin/**")
                .addResourceLocations("file:../admin/")
                .resourceChain(true)
                .addResolver(new PathResourceResolver() {
                    @Override
                    protected Resource getResource(String resourcePath, Resource location) throws IOException {
                        Resource requested = location.createRelative(resourcePath);
                        return requested.exists() && requested.isReadable() ? requested : location.createRelative("index.html");
                    }
                });
    }
}
