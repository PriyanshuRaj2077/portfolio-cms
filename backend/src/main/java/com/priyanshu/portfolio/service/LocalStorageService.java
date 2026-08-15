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

    public LocalStorageService(@Value("${portfolio.publish.output-dir}") String outputDir) {
        this.outputDirectory = Paths.get(outputDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.outputDirectory);
        } catch (Exception e) {
            throw new RuntimeException("Could not initialize publishing output directory", e);
        }
    }

    @Override
    public void saveFile(String filename, byte[] content) throws Exception {
        Path targetPath = this.outputDirectory.resolve(filename);
        Files.write(targetPath, content);
    }

    @Override
    public boolean verifyFileExists(String filename) {
        Path targetPath = this.outputDirectory.resolve(filename);
        File file = targetPath.toFile();
        return file.exists() && file.length() > 0;
    }
}
