package com.priyanshu.portfolio.controller;

import com.priyanshu.portfolio.service.StorageService;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.concurrent.TimeUnit;

@RestController
public class PublicContentController {

    private final StorageService storageService;

    public PublicContentController(StorageService storageService) {
        this.storageService = storageService;
    }

    /**
     * Resilient public endpoint for published portfolio JSON files.
     * Serves manifest and versioned files from persistent storage with optimal HTTP caching.
     */
    @GetMapping(value = "/data/published/default/{filename}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getPublishedFile(@PathVariable String filename) {
        // Prevent path traversal
        if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
            return ResponseEntity.badRequest().build();
        }

        try {
            byte[] content = storageService.readFile(filename);
            if (content == null) {
                return ResponseEntity.notFound().build();
            }

            CacheControl cacheControl;
            if ("manifest.json".equalsIgnoreCase(filename)) {
                cacheControl = CacheControl.noCache().noStore().mustRevalidate();
            } else {
                cacheControl = CacheControl.maxAge(365, TimeUnit.DAYS).cachePublic().immutable();
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .cacheControl(cacheControl)
                    .body(content);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Public media resolution endpoint.
     * Redirects to public CDN URL or streams local dev media.
     */
    @GetMapping("/media/{filename}")
    public ResponseEntity<?> getMediaFile(@PathVariable String filename) {
        if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
            return ResponseEntity.badRequest().build();
        }

        String mediaUrl = storageService.getMediaUrl(filename);
        if (mediaUrl.startsWith("http://") || mediaUrl.startsWith("https://")) {
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create(mediaUrl))
                    .build();
        }

        // In local development, the Spring WebMvc resource handler handles /media/** from disk.
        return ResponseEntity.notFound().build();
    }
}
