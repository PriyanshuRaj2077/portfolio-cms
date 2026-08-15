package com.priyanshu.portfolio.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "blog_posts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BlogPostEntity {
    @Id
    private String id;

    @Column(nullable = false)
    private String title;

    @Column(unique = true)
    private String slug;

    private String date;
    private String readTime;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(columnDefinition = "TEXT")
    private String contentMarkdown;

    private String tagsJson;
    private String status; // DRAFT, PUBLISHED
}
