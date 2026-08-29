package com.priyanshu.portfolio.service;

/**
 * Represents a single usage reference of a media file in content.
 * Used by MediaUsageService to report all locations where a media file is referenced.
 */
public class MediaUsageReference {

    private final String type;  // e.g. "Profile", "Blog", "Project"
    private final String label; // human-readable description

    public MediaUsageReference(String type, String label) {
        this.type = type;
        this.label = label;
    }

    public String getType() { return type; }
    public String getLabel() { return label; }

    /** Returns a display string suitable for API responses and admin UI */
    @Override
    public String toString() {
        return type + ": " + label;
    }
}
