package com.priyanshu.portfolio.storage;

import com.priyanshu.portfolio.service.SupabaseStorageService;
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

class SupabaseStorageServiceTest {

    private S3Client s3Client;
    private SupabaseStorageService supabaseStorageService;
    private final String bucketName = "portfolio";
    private final String publicBaseUrl = "https://xyz.supabase.co/storage/v1/object/public/portfolio";

    @BeforeEach
    void setUp() {
        s3Client = mock(S3Client.class);
        supabaseStorageService = new SupabaseStorageService(s3Client, bucketName, publicBaseUrl);
    }

    @Test
    @DisplayName("saveFile uploads published JSON with correct Supabase key, application/json Content-Type, and cache headers")
    void testSaveFilePublishedJson() throws Exception {
        byte[] content = "{\"version\":2}".getBytes(StandardCharsets.UTF_8);

        supabaseStorageService.saveFile("profile.v2.json", content);

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

        supabaseStorageService.saveFile("manifest.json", content);

        ArgumentCaptor<PutObjectRequest> requestCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client).putObject(requestCaptor.capture(), any(RequestBody.class));

        PutObjectRequest capturedRequest = requestCaptor.getValue();
        assertEquals("data/published/default/manifest.json", capturedRequest.key());
        assertEquals("application/json", capturedRequest.contentType());
        assertTrue(capturedRequest.cacheControl().contains("no-cache"));
    }

    @Test
    @DisplayName("readFile returns byte content when object exists in Supabase storage")
    void testReadFileSuccess() throws Exception {
        byte[] expected = "{\"data\":\"test\"}".getBytes(StandardCharsets.UTF_8);

        GetObjectResponse getResponse = GetObjectResponse.builder().contentLength((long) expected.length).build();
        ResponseBytes<GetObjectResponse> responseBytes = ResponseBytes.fromByteArray(getResponse, expected);

        when(s3Client.getObjectAsBytes(any(GetObjectRequest.class))).thenReturn(responseBytes);

        byte[] result = supabaseStorageService.readFile("profile.v1.json");

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

        byte[] result = supabaseStorageService.readFile("nonexistent.json");
        assertNull(result);
    }

    @Test
    @DisplayName("verifyFileExists returns true when headObject confirms positive contentLength")
    void testVerifyFileExistsTrue() {
        HeadObjectResponse headResponse = HeadObjectResponse.builder().contentLength(1024L).build();
        when(s3Client.headObject(any(HeadObjectRequest.class))).thenReturn(headResponse);

        boolean exists = supabaseStorageService.verifyFileExists("manifest.json");
        assertTrue(exists);
    }

    @Test
    @DisplayName("verifyFileExists returns false when object is missing or 0 bytes")
    void testVerifyFileExistsFalse() {
        when(s3Client.headObject(any(HeadObjectRequest.class)))
                .thenThrow(NoSuchKeyException.builder().message("Not found").build());

        boolean exists = supabaseStorageService.verifyFileExists("missing.json");
        assertFalse(exists);
    }

    @Test
    @DisplayName("saveMedia uploads media file with correct MIME type and immutable caching")
    void testSaveMedia() throws Exception {
        byte[] imageBytes = new byte[]{1, 2, 3, 4};
        String fileName = "avatar.png";

        supabaseStorageService.saveMedia(fileName, imageBytes, "image/png");

        ArgumentCaptor<PutObjectRequest> requestCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client).putObject(requestCaptor.capture(), any(RequestBody.class));

        PutObjectRequest req = requestCaptor.getValue();
        assertEquals(bucketName, req.bucket());
        assertEquals("media/avatar.png", req.key());
        assertEquals("image/png", req.contentType());
        assertEquals("public, max-age=31536000, immutable", req.cacheControl());
    }

    @Test
    @DisplayName("saveMedia single-arg overload defaults content type to application/octet-stream")
    void testSaveMediaDefaultContentType() throws Exception {
        byte[] imageBytes = new byte[]{1, 2, 3, 4};
        String fileName = "binary.dat";

        supabaseStorageService.saveMedia(fileName, imageBytes);

        ArgumentCaptor<PutObjectRequest> requestCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client).putObject(requestCaptor.capture(), any(RequestBody.class));

        PutObjectRequest req = requestCaptor.getValue();
        assertEquals(bucketName, req.bucket());
        assertEquals("media/binary.dat", req.key());
        assertEquals("application/octet-stream", req.contentType());
    }

    @Test
    @DisplayName("readFile returns null on S3Exception with 404 status code")
    void testReadFileS3Exception404() throws Exception {
        S3Exception ex404 = (S3Exception) S3Exception.builder().statusCode(404).message("Not Found").build();
        when(s3Client.getObjectAsBytes(any(GetObjectRequest.class))).thenThrow(ex404);

        byte[] result = supabaseStorageService.readFile("missing.json");
        assertNull(result);
    }

    @Test
    @DisplayName("readFile rethrows S3Exception on server error (500)")
    void testReadFileS3Exception500() {
        S3Exception ex500 = (S3Exception) S3Exception.builder().statusCode(500).message("Internal Error").build();
        when(s3Client.getObjectAsBytes(any(GetObjectRequest.class))).thenThrow(ex500);

        assertThrows(S3Exception.class, () -> supabaseStorageService.readFile("error.json"));
    }

    @Test
    @DisplayName("verifyFileExists returns false on S3Exception with 404 status")
    void testVerifyFileExistsS3Exception404() {
        S3Exception ex404 = (S3Exception) S3Exception.builder().statusCode(404).message("Not Found").build();
        when(s3Client.headObject(any(HeadObjectRequest.class))).thenThrow(ex404);

        boolean exists = supabaseStorageService.verifyFileExists("missing.json");
        assertFalse(exists);
    }

    @Test
    @DisplayName("getMediaUrl and getPublishBaseUrl generate correct Supabase Storage public URLs")
    void testUrlGeneration() {
        assertEquals("https://xyz.supabase.co/storage/v1/object/public/portfolio/media/photo.jpg",
                supabaseStorageService.getMediaUrl("photo.jpg"));
        assertEquals("https://xyz.supabase.co/storage/v1/object/public/portfolio/data/published/default",
                supabaseStorageService.getPublishBaseUrl());
    }

    @Test
    @DisplayName("derivePublicBaseUrl correctly formats URLs across various endpoint conventions")
    void testDerivePublicBaseUrl() {
        assertEquals("https://xyz.supabase.co/storage/v1/object/public/portfolio",
                SupabaseStorageService.derivePublicBaseUrl("https://xyz.supabase.co/storage/v1/s3", "portfolio"));

        assertEquals("https://xyz.supabase.co/storage/v1/object/public/portfolio",
                SupabaseStorageService.derivePublicBaseUrl("https://xyz.supabase.co/storage/v1/s3/", "portfolio"));

        assertEquals("https://xyz.supabase.co/storage/v1/object/public/portfolio",
                SupabaseStorageService.derivePublicBaseUrl("https://xyz.supabase.co", "portfolio"));

        assertEquals("http://localhost:8000/storage/v1/object/public/portfolio",
                SupabaseStorageService.derivePublicBaseUrl("http://localhost:8000/storage/v1/s3", "portfolio"));

        assertEquals("",
                SupabaseStorageService.derivePublicBaseUrl(null, "portfolio"));

        assertEquals("",
                SupabaseStorageService.derivePublicBaseUrl("   ", "portfolio"));
    }

    @Test
    @DisplayName("deleteMedia invokes S3Client deleteObject")
    void testDeleteMedia() throws Exception {
        boolean result = supabaseStorageService.deleteMedia("photo.jpg");

        assertTrue(result);
        ArgumentCaptor<DeleteObjectRequest> captor = ArgumentCaptor.forClass(DeleteObjectRequest.class);
        verify(s3Client).deleteObject(captor.capture());
        assertEquals("media/photo.jpg", captor.getValue().key());
    }

    @Test
    @DisplayName("deleteMedia returns false when S3Client throws exception")
    void testDeleteMediaFailure() throws Exception {
        doThrow(new RuntimeException("S3 Delete Failure")).when(s3Client).deleteObject(any(DeleteObjectRequest.class));

        boolean result = supabaseStorageService.deleteMedia("photo.jpg");
        assertFalse(result);
    }
}
