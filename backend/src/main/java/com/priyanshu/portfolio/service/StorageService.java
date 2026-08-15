package com.priyanshu.portfolio.service;

public interface StorageService {
    void saveFile(String filename, byte[] content) throws Exception;
    boolean verifyFileExists(String filename);
}
