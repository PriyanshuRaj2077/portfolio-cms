package com.priyanshu.portfolio.storage;

import com.priyanshu.portfolio.service.R2StorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class R2StorageServiceTest {

    private S3Client s3Client;
    private R2StorageService r2StorageService;
    private final String bucketName = "portfolio-bucket";
    private final String publicBaseUrl = "https://pub-r2.example.com";

    @BeforeEach
    void setUp() {
        s3Client = mock(S3Client.class);
        r2StorageService = new R2StorageService(s3Client, bucketName, publicBaseUrl);
    }

    @Test
    @DisplayName("saveFile uploads published JSON with correct R2 key, application/json Content-Type, and cache headers")
    void testSaveFilePublishedJson() throws Exception {
        byte[] content = "{\"version\":2}".getBytes(StandardCharsets.UTF_8);

        r2StorageService.saveFile("profile.v2.json", content);

        ArgumentCaptor<PutObjectRequest> requestCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
        ArgumentCaptor<RequestBody> bodyCaptor = ArgumentCaptor.forClass(RequestBody.class);

        verify(s3Client).putObject(requestCaptor.capture(), bodyCaptor.capture());

        PutObjectRequest capturedRequest = requestCaptor.getValue();
        assertEquals(bucketName, capturedRequest.bucket());
        assertEquals("data/published/default/profile.v2.json", capturedRequest.key());
        assertEquals("application/json", capturedRequest.contentType());
        assertEquals("public, max-age=31536000, immutable", capturedRequest.cacheControl());

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bodyCaptor.getValue().contentStreamProvider().newStream().transferTo(baos);
        assertArrayEquals(content, baos.toByteArray());
    }

    @Test
    @DisplayName("saveFile uploads manifest.json with no-cache headers")
    void testSaveFileManifestJson() throws Exception {
        byte[] content = "{\"version\":2}".getBytes(StandardCharsets.UTF_8);

        r2StorageService.saveFile("manifest.json", content);

        ArgumentCaptor<PutObjectRequest> requestCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client).putObject(requestCaptor.capture(), any(RequestBody.class));

        PutObjectRequest capturedRequest = requestCaptor.getValue();
        assertEquals("data/published/default/manifest.json", capturedRequest.key());
        assertEquals("application/json", capturedRequest.contentType());
        assertTrue(capturedRequest.cacheControl().contains("no-cache"));
    }

    @Test
    @DisplayName("readFile returns byte content when object exists in R2")
    void testReadFileSuccess() throws Exception {
        byte[] expected = "{\"data\":\"test\"}".getBytes(StandardCharsets.UTF_8);

        GetObjectResponse getResponse = GetObjectResponse.builder().contentLength((long) expected.length).build();
        ResponseBytes<GetObjectResponse> responseBytes = ResponseBytes.fromByteArray(getResponse, expected);

        when(s3Client.getObjectAsBytes(any(GetObjectRequest.class))).thenReturn(responseBytes);

        byte[] result = r2StorageService.readFile("profile.v1.json");

        assertNotNull(result);
        assertArrayEquals(expected, result);

        ArgumentCaptor<GetObjectRequest> requestCaptor = ArgumentCaptor.forClass(GetObjectRequest.class);
        verify(s3Client).getObjectAsBytes(requestCaptor.capture());
        assertEquals(bucketName, requestCaptor.getValue().bucket());
        assertEquals("data/published/default/profile.v1.json", requestCaptor.getValue().key());
    }

    @Test
    @DisplayName("readFile returns null when NoSuchKeyException is thrown")
    void testReadFileNoSuchKey() throws Exception {
        when(s3Client.getObjectAsBytes(any(GetObjectRequest.class)))
                .thenThrow(NoSuchKeyException.builder().message("Key not found").build());

        byte[] result = r2StorageService.readFile("nonexistent.json");
        assertNull(result);
    }

    @Test
    @DisplayName("verifyFileExists returns true when headObject confirms positive contentLength")
    void testVerifyFileExistsTrue() {
        HeadObjectResponse headResponse = HeadObjectResponse.builder().contentLength(1024L).build();
        when(s3Client.headObject(any(HeadObjectRequest.class))).thenReturn(headResponse);

        boolean exists = r2StorageService.verifyFileExists("manifest.json");
        assertTrue(exists);
    }

    @Test
    @DisplayName("verifyFileExists returns false when object is missing or 0 bytes")
    void testVerifyFileExistsFalse() {
        when(s3Client.headObject(any(HeadObjectRequest.class)))
                .thenThrow(NoSuchKeyException.builder().message("Not found").build());

        boolean exists = r2StorageService.verifyFileExists("missing.json");
        assertFalse(exists);
    }

    @Test
    @DisplayName("saveMedia uploads media file with correct MIME type and immutable caching")
    void testSaveMedia() throws Exception {
        byte[] imageBytes = new byte[]{1, 2, 3, 4};
        String fileName = "avatar.png";

        r2StorageService.saveMedia(fileName, imageBytes, "image/png");

        ArgumentCaptor<PutObjectRequest> requestCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client).putObject(requestCaptor.capture(), any(RequestBody.class));

        PutObjectRequest req = requestCaptor.getValue();
        assertEquals(bucketName, req.bucket());
        assertEquals("media/avatar.png", req.key());
        assertEquals("image/png", req.contentType());
        assertEquals("public, max-age=31536000, immutable", req.cacheControl());
    }

    @Test
    @DisplayName("getMediaUrl and getPublishBaseUrl generate correct CDN URLs")
    void testUrlGeneration() {
        assertEquals("https://pub-r2.example.com/media/photo.jpg", r2StorageService.getMediaUrl("photo.jpg"));
        assertEquals("https://pub-r2.example.com/data/published/default", r2StorageService.getPublishBaseUrl());
    }

    @Test
    @DisplayName("deleteMedia invokes S3Client deleteObject")
    void testDeleteMedia() throws Exception {
        boolean result = r2StorageService.deleteMedia("photo.jpg");

        assertTrue(result);
        ArgumentCaptor<DeleteObjectRequest> captor = ArgumentCaptor.forClass(DeleteObjectRequest.class);
        verify(s3Client).deleteObject(captor.capture());
        assertEquals("media/photo.jpg", captor.getValue().key());
    }
}
