package com.priyanshu.portfolio.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "comments", indexes = {
    @Index(name = "idx_comments_article_status", columnList = "article_id, status"),
    @Index(name = "idx_comments_submitter_token", columnList = "submitter_token")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommentEntity {

    @Id
    private String id; // UUID string generated before save

    @Column(name = "article_id", nullable = false)
    private String articleId; // FK to blog_posts.id

    @Column(name = "author_name", nullable = false, length = 100)
    private String authorName;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    /**
     * PENDING = awaiting admin review (private, not shown publicly)
     * APPROVED = visible in public comment list
     */
    @Column(nullable = false, length = 20)
    private String status; // PENDING | APPROVED

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /**
     * Secure random UUID issued once at submission time.
     * Returned only in the HTTP response body at submission.
     * Never re-exposed via public GET endpoints.
     * Allows the submitting browser/session to identify its own pending comment.
     */
    @Column(name = "submitter_token", nullable = false, length = 64)
    private String submitterToken;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.status == null) {
            this.status = "PENDING";
        }
    }
}
