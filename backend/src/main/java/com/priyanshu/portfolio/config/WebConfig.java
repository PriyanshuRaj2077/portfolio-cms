package com.priyanshu.portfolio.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.*;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;

@Configuration
public class WebConfig implements WebMvcConfigurer {

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
                .addResourceLocations("file:../admin/")
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


