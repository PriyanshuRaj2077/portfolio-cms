package com.priyanshu.portfolio.service;

public interface StorageService {
    void saveFile(String filename, byte[] content) throws Exception;
    byte[] readFile(String filename) throws Exception;
    boolean verifyFileExists(String filename);

    void saveMedia(String filename, byte[] content) throws Exception;
    boolean deleteMedia(String filename) throws Exception;
}

