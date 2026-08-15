package com.priyanshu.portfolio.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "projects")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectEntity {
    @Id
    private String id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String tagsJson; // Comma separated or JSON string
    private String coverImage;
    private String repoUrl;
    private String liveUrl;
    private String status;
    private Boolean featured;
    private Integer sortOrder;
}
