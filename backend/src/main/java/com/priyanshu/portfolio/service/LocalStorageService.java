package com.priyanshu.portfolio.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class LocalStorageService implements StorageService {

    private final Path outputDirectory;
    private final Path mediaDirectory;

    public LocalStorageService(
            @Value("${portfolio.publish.output-dir:../frontend/data/published/default}") String outputDir,
            @Value("${portfolio.media.output-dir:../frontend/media}") String mediaDir
    ) {
        this.outputDirectory = Paths.get(outputDir).toAbsolutePath().normalize();
        this.mediaDirectory = Paths.get(mediaDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.outputDirectory);
            Files.createDirectories(this.mediaDirectory);
        } catch (Exception e) {
            throw new RuntimeException("Could not initialize storage directories", e);
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
        return null;
    }

    @Override
    public boolean verifyFileExists(String filename) {
        Path targetPath = this.outputDirectory.resolve(filename);
        File file = targetPath.toFile();
        return file.exists() && file.length() > 0;
    }

    @Override
    public void saveMedia(String filename, byte[] content) throws Exception {
        Path targetPath = this.mediaDirectory.resolve(filename);
        Files.write(targetPath, content);
    }

    @Override
    public boolean deleteMedia(String filename) throws Exception {
        Path targetPath = this.mediaDirectory.resolve(filename);
        return Files.deleteIfExists(targetPath);
    }
}

