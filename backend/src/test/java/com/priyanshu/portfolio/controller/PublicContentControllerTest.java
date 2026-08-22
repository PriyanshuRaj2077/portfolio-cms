package com.priyanshu.portfolio.controller;

import com.priyanshu.portfolio.service.StorageService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "admin.username=testadmin",
        "admin.initial-password=SecurePass123!"
})
@ActiveProfiles("test")
@AutoConfigureMockMvc
class PublicContentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StorageService storageService;

    @Test
    @DisplayName("GET /data/published/default/manifest.json returns 200 with no-cache headers when present")
    void testGetManifestSuccess() throws Exception {
        byte[] content = "{\"version\":2,\"files\":{}}".getBytes(StandardCharsets.UTF_8);
        when(storageService.readFile("manifest.json")).thenReturn(content);

        mockMvc.perform(get("/data/published/default/manifest.json"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
                .andExpect(jsonPath("$.version").value(2));
    }

    @Test
    @DisplayName("GET /data/published/default/profile.v2.json returns 200 with immutable long cache headers")
    void testGetVersionedFileSuccess() throws Exception {
        byte[] content = "{\"name\":\"PRIYANSHU\"}".getBytes(StandardCharsets.UTF_8);
        when(storageService.readFile("profile.v2.json")).thenReturn(content);

        mockMvc.perform(get("/data/published/default/profile.v2.json"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(header().exists("Cache-Control"))
                .andExpect(jsonPath("$.name").value("PRIYANSHU"));
    }

    @Test
    @DisplayName("GET /data/published/default/missing.json returns 404 when file not in storage")
    void testGetMissingFile404() throws Exception {
        when(storageService.readFile("missing.json")).thenReturn(null);

        mockMvc.perform(get("/data/published/default/missing.json"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /data/published/default/../secret.json rejects path traversal with 400 Bad Request")
    void testPathTraversalRejected() throws Exception {
        mockMvc.perform(get("/data/published/default/..%2Fsecret.json"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /media/{filename} redirects (302) when CDN URL is available")
    void testMediaRedirectToCdn() throws Exception {
        when(storageService.getMediaUrl("photo.png")).thenReturn("https://xyz.supabase.co/storage/v1/object/public/portfolio/media/photo.png");

        mockMvc.perform(get("/media/photo.png"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "https://xyz.supabase.co/storage/v1/object/public/portfolio/media/photo.png"));
    }
}
