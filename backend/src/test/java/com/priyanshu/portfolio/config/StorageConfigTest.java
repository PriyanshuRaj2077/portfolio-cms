package com.priyanshu.portfolio.config;

import com.priyanshu.portfolio.service.LocalStorageService;
import com.priyanshu.portfolio.service.StorageService;
import com.priyanshu.portfolio.service.SupabaseStorageService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class StorageConfigTest {

    @Test
    @DisplayName("StorageConfig returns LocalStorageService when test or dev profile is active")
    void testDevProfileActivatesLocalStorageService() {
        StorageConfig config = new StorageConfig();
        ReflectionTestUtils.setField(config, "localOutputDir", "target/test-out");
        ReflectionTestUtils.setField(config, "localMediaDir", "target/test-media");
        ReflectionTestUtils.setField(config, "localPublishPublicBaseUrl", "/data/published/default");
        ReflectionTestUtils.setField(config, "localMediaPublicBaseUrl", "/media");

        Environment env = mock(Environment.class);
        when(env.getActiveProfiles()).thenReturn(new String[]{"dev"});

        StorageService service = config.storageService(env);
        assertNotNull(service);
        assertInstanceOf(LocalStorageService.class, service);
    }

    @Test
    @DisplayName("StorageConfig fails fast in production when Supabase S3 environment variables are missing")
    void testProductionFailsFastWithoutSupabaseVariables() {
        StorageConfig config = new StorageConfig();
        // Leave Supabase fields empty/blank

        Environment env = mock(Environment.class);
        when(env.getActiveProfiles()).thenReturn(new String[]{"prod"});

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> config.storageService(env));
        assertTrue(ex.getMessage().contains("FATAL PRODUCTION STORAGE CONFIGURATION"));
        assertTrue(ex.getMessage().contains("SUPABASE_S3_ENDPOINT"));
        assertTrue(ex.getMessage().contains("SUPABASE_S3_REGION"));
        assertTrue(ex.getMessage().contains("SUPABASE_S3_ACCESS_KEY_ID"));
        assertTrue(ex.getMessage().contains("SUPABASE_S3_SECRET_ACCESS_KEY"));
        assertTrue(ex.getMessage().contains("SUPABASE_S3_BUCKET"));
    }

    @Test
    @DisplayName("StorageConfig constructs SupabaseStorageService when in production and all Supabase variables are set")
    void testProductionConstructsSupabaseStorageService() {
        StorageConfig config = new StorageConfig();
        ReflectionTestUtils.setField(config, "supabaseEndpoint", "https://xyz.supabase.co/storage/v1/s3");
        ReflectionTestUtils.setField(config, "supabaseRegion", "us-east-1");
        ReflectionTestUtils.setField(config, "supabaseAccessKeyId", "dummyAccessKey");
        ReflectionTestUtils.setField(config, "supabaseSecretAccessKey", "dummySecretKey");
        ReflectionTestUtils.setField(config, "supabaseBucket", "portfolio");

        Environment env = mock(Environment.class);
        when(env.getActiveProfiles()).thenReturn(new String[]{"prod"});

        StorageService service = config.storageService(env);
        assertNotNull(service);
        assertInstanceOf(SupabaseStorageService.class, service);
        assertEquals("https://xyz.supabase.co/storage/v1/object/public/portfolio/media/test.png", service.getMediaUrl("test.png"));
    }
    @Test
    @DisplayName("StorageConfig returns LocalStorageService when test profile is active")
    void testTestProfileActivatesLocalStorageService() {
        StorageConfig config = new StorageConfig();
        ReflectionTestUtils.setField(config, "localOutputDir", "target/test-out");
        ReflectionTestUtils.setField(config, "localMediaDir", "target/test-media");
        ReflectionTestUtils.setField(config, "localPublishPublicBaseUrl", "/data/published/default");
        ReflectionTestUtils.setField(config, "localMediaPublicBaseUrl", "/media");

        Environment env = mock(Environment.class);
        when(env.getActiveProfiles()).thenReturn(new String[]{"test"});

        StorageService service = config.storageService(env);
        assertNotNull(service);
        assertInstanceOf(LocalStorageService.class, service);
    }

    @Test
    @DisplayName("StorageConfig fails fast when specific Supabase environment variable is missing")
    void testProductionFailsFastWhenSingleVariableMissing() {
        StorageConfig config = new StorageConfig();
        ReflectionTestUtils.setField(config, "supabaseEndpoint", "https://xyz.supabase.co/storage/v1/s3");
        ReflectionTestUtils.setField(config, "supabaseRegion", "us-east-1");
        ReflectionTestUtils.setField(config, "supabaseAccessKeyId", "dummyAccessKey");
        ReflectionTestUtils.setField(config, "supabaseSecretAccessKey", "dummySecretKey");
        // Leave supabaseBucket null/blank

        Environment env = mock(Environment.class);
        when(env.getActiveProfiles()).thenReturn(new String[]{"prod"});

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> config.storageService(env));
        assertTrue(ex.getMessage().contains("SUPABASE_S3_BUCKET"));
    }
}
