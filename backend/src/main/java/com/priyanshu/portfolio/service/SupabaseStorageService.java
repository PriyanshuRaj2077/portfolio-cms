package com.priyanshu.portfolio.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.*;

import java.net.URI;

public class SupabaseStorageService implements StorageService {

    private static final Logger log = LoggerFactory.getLogger(SupabaseStorageService.class);

    private final S3Client s3Client;
    private final String bucket;
    private final String publicBaseUrl;

    public SupabaseStorageService(
            String endpoint,
            String region,
            String accessKeyId,
            String secretAccessKey,
            String bucket
    ) {
        this.bucket = bucket != null ? bucket.trim() : "";
        String trimmedEndpoint = endpoint != null ? endpoint.trim() : "";
        String trimmedRegion = region != null ? region.trim() : "us-east-1";
        this.publicBaseUrl = derivePublicBaseUrl(trimmedEndpoint, this.bucket);

        log.info("Initializing Supabase S3 Storage Service for bucket: '{}' at endpoint: '{}' (region: '{}')",
                this.bucket, trimmedEndpoint, trimmedRegion);

        this.s3Client = S3Client.builder()
                .endpointOverride(URI.create(trimmedEndpoint))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKeyId.trim(), secretAccessKey.trim())
                ))
                .region(Region.of(trimmedRegion))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(true)
                        .chunkedEncodingEnabled(false)
                        .build())
                .build();
    }

    public SupabaseStorageService(S3Client s3Client, String bucket, String publicBaseUrl) {
        this.s3Client = s3Client;
        this.bucket = bucket != null ? bucket.trim() : "";
        this.publicBaseUrl = publicBaseUrl != null ? publicBaseUrl.trim().replaceAll("/+$", "") : "";
    }

    public static String derivePublicBaseUrl(String endpoint, String bucket) {
        if (endpoint == null || endpoint.isBlank()) {
            return "";
        }
        String cleaned = endpoint.trim().replaceAll("/+$", "");
        String bucketClean = bucket != null ? bucket.trim() : "";

        if (cleaned.endsWith("/storage/v1/s3")) {
            return cleaned.substring(0, cleaned.length() - "/storage/v1/s3".length())
                    + "/storage/v1/object/public/" + bucketClean;
        } else if (cleaned.endsWith("/s3")) {
            return cleaned.substring(0, cleaned.length() - "/s3".length())
                    + "/object/public/" + bucketClean;
        } else if (!cleaned.contains("/storage/v1")) {
            return cleaned + "/storage/v1/object/public/" + bucketClean;
        } else {
            return cleaned + "/object/public/" + bucketClean;
        }
    }

    private String resolvePublishKey(String filename) {
        return "data/published/default/" + filename;
    }

    private String resolveMediaKey(String filename) {
        return "media/" + filename;
    }

    @Override
    public void saveFile(String filename, byte[] content) throws Exception {
        String key = resolvePublishKey(filename);
        String contentType = filename.endsWith(".json") ? "application/json" : "application/octet-stream";
        String cacheControl = filename.equals("manifest.json")
                ? "no-cache, no-store, must-revalidate, max-age=0"
                : "public, max-age=31536000, immutable";

        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(this.bucket)
                .key(key)
                .contentType(contentType)
                .cacheControl(cacheControl)
                .build();

        s3Client.putObject(request, RequestBody.fromBytes(content));
        log.debug("Successfully saved published file to Supabase storage: {} ({} bytes)", key, content.length);
    }

    @Override
    public byte[] readFile(String filename) throws Exception {
        String key = resolvePublishKey(filename);
        try {
            GetObjectRequest request = GetObjectRequest.builder()
                    .bucket(this.bucket)
                    .key(key)
                    .build();

            ResponseBytes<GetObjectResponse> bytes = s3Client.getObjectAsBytes(request);
            return bytes.asByteArray();
        } catch (NoSuchKeyException e) {
            log.debug("Supabase storage object not found: {}", key);
            return null;
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                return null;
            }
            log.error("Error reading file from Supabase storage: {}", key, e);
            throw e;
        }
    }

    @Override
    public boolean verifyFileExists(String filename) {
        String key = resolvePublishKey(filename);
        try {
            HeadObjectRequest request = HeadObjectRequest.builder()
                    .bucket(this.bucket)
                    .key(key)
                    .build();

            HeadObjectResponse response = s3Client.headObject(request);
            return response.contentLength() != null && response.contentLength() > 0;
        } catch (NoSuchKeyException e) {
            return false;
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                return false;
            }
            log.warn("S3Exception verifying file existence in Supabase storage for key: {}", key, e);
            return false;
        }
    }

    @Override
    public void saveMedia(String filename, byte[] content) throws Exception {
        saveMedia(filename, content, "application/octet-stream");
    }

    @Override
    public void saveMedia(String filename, byte[] content, String contentType) throws Exception {
        String key = resolveMediaKey(filename);
        String resolvedContentType = (contentType != null && !contentType.isBlank())
                ? contentType
                : "application/octet-stream";

        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(this.bucket)
                .key(key)
                .contentType(resolvedContentType)
                .cacheControl("public, max-age=31536000, immutable")
                .build();

        s3Client.putObject(request, RequestBody.fromBytes(content));
        log.info("Successfully uploaded media to Supabase storage: {} ({}, {} bytes)", key, resolvedContentType, content.length);
    }

    @Override
    public String getMediaUrl(String filename) {
        return this.publicBaseUrl + "/media/" + filename;
    }

    @Override
    public boolean deleteMedia(String filename) throws Exception {
        String key = resolveMediaKey(filename);
        try {
            DeleteObjectRequest request = DeleteObjectRequest.builder()
                    .bucket(this.bucket)
                    .key(key)
                    .build();

            s3Client.deleteObject(request);
            log.info("Successfully deleted media from Supabase storage: {}", key);
            return true;
        } catch (Exception e) {
            log.error("Failed to delete media from Supabase storage: {}", key, e);
            return false;
        }
    }

    @Override
    public String getPublishBaseUrl() {
        return this.publicBaseUrl + "/data/published/default";
    }
}
