package com.priyanshu.portfolio.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.*;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${portfolio.cors.frontend-origin:}")
    private String frontendOrigin;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        List<String> allowedOrigins = new ArrayList<>();
        if (frontendOrigin != null && !frontendOrigin.isBlank()) {
            String[] origins = frontendOrigin.split(",");
            for (String origin : origins) {
                if (!origin.trim().isEmpty()) {
                    allowedOrigins.add(origin.trim());
                }
            }
        }

        if (allowedOrigins.isEmpty()) {
            allowedOrigins = List.of(
                    "http://localhost:3000",
                    "http://localhost:8080",
                    "http://127.0.0.1:5500",
                    "http://localhost:5173"
            );
        }

        registry.addMapping("/**")
                .allowedOrigins(allowedOrigins.toArray(new String[0]))
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/").setViewName("forward:/index.html");
        registry.addViewController("/admin").setViewName("forward:/admin/index.html");
        registry.addViewController("/admin/").setViewName("forward:/admin/index.html");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 1. Serve media files under /media/**
        registry.addResourceHandler("/media/**")
                .addResourceLocations("file:../frontend/media/")
                .resourceChain(true)
                .addResolver(new PathResourceResolver());

        // 2. Serve admin static frontend under /admin/**
        registry.addResourceHandler("/admin/**")
                .addResourceLocations("file:/app/admin/")
                .resourceChain(true)
                .addResolver(new PathResourceResolver() {
                    @Override
                    protected Resource getResource(String resourcePath, Resource location) throws IOException {
                        Resource requested = location.createRelative(resourcePath);
                        return (requested.exists() && requested.isReadable()) ? requested : location.createRelative("index.html");
                    }
                });

        // 3. Serve blog routes under /blog/** (SPA routing to public index.html)
        registry.addResourceHandler("/blog/**")
                .addResourceLocations("file:../frontend/")
                .resourceChain(true)
                .addResolver(new PathResourceResolver() {
                    @Override
                    protected Resource getResource(String resourcePath, Resource location) throws IOException {
                        return location.createRelative("index.html");
                    }
                });

        // 4. Serve public static frontend from ../frontend/ or classpath
        registry.addResourceHandler("/**")
                .addResourceLocations("file:../frontend/", "classpath:/static/")
                .resourceChain(true)
                .addResolver(new PathResourceResolver() {
                    @Override
                    protected Resource getResource(String resourcePath, Resource location) throws IOException {
                        Resource requested = location.createRelative(resourcePath);
                        if (requested.exists() && requested.isReadable()) {
                            return requested;
                        }
                        if (resourcePath.isEmpty() || resourcePath.equals("/") || resourcePath.startsWith("blog")) {
                            return location.createRelative("index.html");
                        }
                        return null;
                    }
                });
    }
}
