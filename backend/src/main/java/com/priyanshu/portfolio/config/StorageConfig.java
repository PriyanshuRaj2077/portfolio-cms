package com.priyanshu.portfolio.config;

import com.priyanshu.portfolio.service.LocalStorageService;
import com.priyanshu.portfolio.service.StorageService;
import com.priyanshu.portfolio.service.SupabaseStorageService;
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

    @Value("${portfolio.supabase.s3.endpoint:}")
    private String supabaseEndpoint;

    @Value("${portfolio.supabase.s3.region:}")
    private String supabaseRegion;

    @Value("${portfolio.supabase.s3.access-key-id:}")
    private String supabaseAccessKeyId;

    @Value("${portfolio.supabase.s3.secret-access-key:}")
    private String supabaseSecretAccessKey;

    @Value("${portfolio.supabase.s3.bucket:}")
    private String supabaseBucket;

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

        // Production Profile - Supabase S3 Storage is STRICTLY REQUIRED
        List<String> missingConfigs = new ArrayList<>();
        if (supabaseEndpoint == null || supabaseEndpoint.isBlank()) missingConfigs.add("SUPABASE_S3_ENDPOINT (portfolio.supabase.s3.endpoint)");
        if (supabaseRegion == null || supabaseRegion.isBlank()) missingConfigs.add("SUPABASE_S3_REGION (portfolio.supabase.s3.region)");
        if (supabaseAccessKeyId == null || supabaseAccessKeyId.isBlank()) missingConfigs.add("SUPABASE_S3_ACCESS_KEY_ID (portfolio.supabase.s3.access-key-id)");
        if (supabaseSecretAccessKey == null || supabaseSecretAccessKey.isBlank()) missingConfigs.add("SUPABASE_S3_SECRET_ACCESS_KEY (portfolio.supabase.s3.secret-access-key)");
        if (supabaseBucket == null || supabaseBucket.isBlank()) missingConfigs.add("SUPABASE_S3_BUCKET (portfolio.supabase.s3.bucket)");

        if (!missingConfigs.isEmpty()) {
            throw new IllegalStateException(
                "FATAL PRODUCTION STORAGE CONFIGURATION: Supabase S3 Storage is strictly required for production storage. " +
                "Local filesystem persistence is disallowed in production. Missing required environment variables: " +
                missingConfigs
            );
        }

        log.info("Configuring Production Supabase S3 Storage Service for bucket '{}'", supabaseBucket);
        return new SupabaseStorageService(
                supabaseEndpoint,
                supabaseRegion,
                supabaseAccessKeyId,
                supabaseSecretAccessKey,
                supabaseBucket
        );
    }
}
