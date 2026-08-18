package com.priyanshu.portfolio.repository;

import com.priyanshu.portfolio.entity.BlogPostEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface BlogPostRepository extends JpaRepository<BlogPostEntity, String> {
    List<BlogPostEntity> findAllByStatus(String status);
    Optional<BlogPostEntity> findBySlug(String slug);
}

