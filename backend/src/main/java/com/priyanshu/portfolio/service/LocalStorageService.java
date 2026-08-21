package com.priyanshu.portfolio.service;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class LocalStorageService implements StorageService {

    private final Path outputDirectory;
    private final Path mediaDirectory;
    private final Path fallbackDataDirectory;
    private final String publishPublicBaseUrl;
    private final String mediaPublicBaseUrl;

    public LocalStorageService(
            String outputDir,
            String mediaDir,
            String publishPublicBaseUrl,
            String mediaPublicBaseUrl
    ) {
        this.outputDirectory = Paths.get(outputDir).toAbsolutePath().normalize();
        this.mediaDirectory = Paths.get(mediaDir).toAbsolutePath().normalize();
        this.fallbackDataDirectory = Paths.get("../frontend/data/published/default").toAbsolutePath().normalize();
        this.publishPublicBaseUrl = publishPublicBaseUrl != null ? publishPublicBaseUrl.replaceAll("/+$", "") : "/data/published/default";
        this.mediaPublicBaseUrl = mediaPublicBaseUrl != null ? mediaPublicBaseUrl.replaceAll("/+$", "") : "/media";

        try {
            Files.createDirectories(this.outputDirectory);
            Files.createDirectories(this.mediaDirectory);
        } catch (Exception e) {
            throw new RuntimeException("Could not initialize local storage directories", e);
        }
    }

    @Override
    public void saveFile(String filename, byte[] content) throws Exception {
        Path targetPath = this.outputDirectory.resolve(filename);
        Files.write(targetPath, content);
    }

    @Override
    public byte[] readFile(String filename) throws Exception {
        Path targetPath = this.outputDirectory.resolve(filename);
        if (Files.exists(targetPath)) {
            return Files.readAllBytes(targetPath);
        }
        Path fallback = this.fallbackDataDirectory.resolve(filename);
        if (Files.exists(fallback)) {
            return Files.readAllBytes(fallback);
        }
        return null;
    }

    @Override
    public boolean verifyFileExists(String filename) {
        Path targetPath = this.outputDirectory.resolve(filename);
        File file = targetPath.toFile();
        if (file.exists() && file.length() > 0) {
            return true;
        }
        Path fallback = this.fallbackDataDirectory.resolve(filename);
        File fallbackFile = fallback.toFile();
        return fallbackFile.exists() && fallbackFile.length() > 0;
    }

    @Override
    public void saveMedia(String filename, byte[] content) throws Exception {
        saveMedia(filename, content, null);
    }

    @Override
    public void saveMedia(String filename, byte[] content, String contentType) throws Exception {
        Path targetPath = this.mediaDirectory.resolve(filename);
        Files.write(targetPath, content);
    }

    @Override
    public String getMediaUrl(String filename) {
        return this.mediaPublicBaseUrl + "/" + filename;
    }

    @Override
    public boolean deleteMedia(String filename) throws Exception {
        Path targetPath = this.mediaDirectory.resolve(filename);
        return Files.deleteIfExists(targetPath);
    }

    @Override
    public String getPublishBaseUrl() {
        return this.publishPublicBaseUrl;
    }
}
