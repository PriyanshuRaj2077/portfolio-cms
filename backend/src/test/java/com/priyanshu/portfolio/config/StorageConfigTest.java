package com.priyanshu.portfolio.config;

import com.priyanshu.portfolio.service.LocalStorageService;
import com.priyanshu.portfolio.service.R2StorageService;
import com.priyanshu.portfolio.service.StorageService;
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
    @DisplayName("StorageConfig fails fast in production when R2 environment variables are missing")
    void testProductionFailsFastWithoutR2Variables() {
        StorageConfig config = new StorageConfig();
        // Leave R2 fields empty/blank

        Environment env = mock(Environment.class);
        when(env.getActiveProfiles()).thenReturn(new String[]{"prod"});

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> config.storageService(env));
        assertTrue(ex.getMessage().contains("FATAL PRODUCTION STORAGE CONFIGURATION"));
        assertTrue(ex.getMessage().contains("R2_ENDPOINT"));
        assertTrue(ex.getMessage().contains("R2_ACCESS_KEY_ID"));
        assertTrue(ex.getMessage().contains("R2_SECRET_ACCESS_KEY"));
        assertTrue(ex.getMessage().contains("R2_BUCKET"));
        assertTrue(ex.getMessage().contains("R2_PUBLIC_BASE_URL"));
    }

    @Test
    @DisplayName("StorageConfig constructs R2StorageService when in production and all R2 variables are set")
    void testProductionConstructsR2StorageService() {
        StorageConfig config = new StorageConfig();
        ReflectionTestUtils.setField(config, "r2Endpoint", "https://123456789.r2.cloudflarestorage.com");
        ReflectionTestUtils.setField(config, "r2AccessKeyId", "dummyAccessKey");
        ReflectionTestUtils.setField(config, "r2SecretAccessKey", "dummySecretKey");
        ReflectionTestUtils.setField(config, "r2Bucket", "my-portfolio-bucket");
        ReflectionTestUtils.setField(config, "r2PublicBaseUrl", "https://pub-r2.example.com");

        Environment env = mock(Environment.class);
        when(env.getActiveProfiles()).thenReturn(new String[]{"prod"});

        StorageService service = config.storageService(env);
        assertNotNull(service);
        assertInstanceOf(R2StorageService.class, service);
        assertEquals("https://pub-r2.example.com/media/test.png", service.getMediaUrl("test.png"));
    }
}
