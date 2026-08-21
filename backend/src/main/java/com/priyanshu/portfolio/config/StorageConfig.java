package com.priyanshu.portfolio.config;

import com.priyanshu.portfolio.service.LocalStorageService;
import com.priyanshu.portfolio.service.R2StorageService;
import com.priyanshu.portfolio.service.StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Configuration
public class StorageConfig {

    private static final Logger log = LoggerFactory.getLogger(StorageConfig.class);

    @Value("${portfolio.r2.endpoint:}")
    private String r2Endpoint;

    @Value("${portfolio.r2.access-key-id:}")
    private String r2AccessKeyId;

    @Value("${portfolio.r2.secret-access-key:}")
    private String r2SecretAccessKey;

    @Value("${portfolio.r2.bucket:}")
    private String r2Bucket;

    @Value("${portfolio.r2.public-base-url:}")
    private String r2PublicBaseUrl;

    @Value("${portfolio.publish.output-dir:../frontend/data/published/default}")
    private String localOutputDir;

    @Value("${portfolio.media.output-dir:../frontend/media}")
    private String localMediaDir;

    @Value("${portfolio.publish.public-base-url:/data/published/default}")
    private String localPublishPublicBaseUrl;

    @Value("${portfolio.media.public-base-url:/media}")
    private String localMediaPublicBaseUrl;

    @Bean
    @Primary
    public StorageService storageService(Environment environment) {
        String[] activeProfiles = environment.getActiveProfiles();
        boolean isDevOrTest = Arrays.stream(activeProfiles)
                .anyMatch(p -> p.equalsIgnoreCase("dev") || p.equalsIgnoreCase("test"));

        if (isDevOrTest) {
            log.info("Active profile is [{}]. Using LocalStorageService for local filesystem persistence.",
                    String.join(",", activeProfiles));
            return new LocalStorageService(
                    localOutputDir,
                    localMediaDir,
                    localPublishPublicBaseUrl,
                    localMediaPublicBaseUrl
            );
        }

        // Production Profile - Cloudflare R2 is STRICTLY REQUIRED
        List<String> missingConfigs = new ArrayList<>();
        if (r2Endpoint == null || r2Endpoint.isBlank()) missingConfigs.add("R2_ENDPOINT (portfolio.r2.endpoint)");
        if (r2AccessKeyId == null || r2AccessKeyId.isBlank()) missingConfigs.add("R2_ACCESS_KEY_ID (portfolio.r2.access-key-id)");
        if (r2SecretAccessKey == null || r2SecretAccessKey.isBlank()) missingConfigs.add("R2_SECRET_ACCESS_KEY (portfolio.r2.secret-access-key)");
        if (r2Bucket == null || r2Bucket.isBlank()) missingConfigs.add("R2_BUCKET (portfolio.r2.bucket)");
        if (r2PublicBaseUrl == null || r2PublicBaseUrl.isBlank()) missingConfigs.add("R2_PUBLIC_BASE_URL (portfolio.r2.public-base-url)");

        if (!missingConfigs.isEmpty()) {
            throw new IllegalStateException(
                "FATAL PRODUCTION STORAGE CONFIGURATION: Cloudflare R2 is strictly required for production storage. " +
                "Local filesystem persistence is disallowed in production. Missing required environment variables: " +
                missingConfigs
            );
        }

        log.info("Configuring Production Cloudflare R2 Storage Service for bucket '{}'", r2Bucket);
        return new R2StorageService(
                r2Endpoint,
                r2AccessKeyId,
                r2SecretAccessKey,
                r2Bucket,
                r2PublicBaseUrl
        );
    }
}
