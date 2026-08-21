package com.priyanshu.portfolio.service;

public interface StorageService {
    /**
     * Save published JSON file (e.g. profile.v2.json, manifest.json) to storage
     */
    void saveFile(String filename, byte[] content) throws Exception;

    /**
     * Read published JSON file from storage
     */
    byte[] readFile(String filename) throws Exception;

    /**
     * Verify whether a published file exists and is non-empty
     */
    boolean verifyFileExists(String filename);

    /**
     * Save media file to storage
     */
    void saveMedia(String filename, byte[] content) throws Exception;

    /**
     * Save media file with specified MIME type
     */
    void saveMedia(String filename, byte[] content, String contentType) throws Exception;

    /**
     * Get the public URL for a media file
     */
    String getMediaUrl(String filename);

    /**
     * Delete media file from storage
     */
    boolean deleteMedia(String filename) throws Exception;

    /**
     * Get public base URL for published content
     */
    String getPublishBaseUrl();
}
