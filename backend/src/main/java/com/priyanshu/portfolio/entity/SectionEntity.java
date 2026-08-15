package com.priyanshu.portfolio.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "sections")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SectionEntity {
    @Id
    private String id;

    @Column(nullable = false)
    private String title;

    private String label;

    @Column(nullable = false)
    private String type; // TEXT, PROJECTS, SKILLS, TIMELINE, ACHIEVEMENTS, BLOG, GALLERY, CONTACT

    @Column(name = "sort_order", nullable = false)
    private Integer order;

    @Column(nullable = false)
    private Boolean visible;

    private String theme; // default, orange, purple

    private String description;

    @Column(columnDefinition = "TEXT")
    private String contentData;
}
