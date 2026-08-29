package com.priyanshu.portfolio.repository;

import com.priyanshu.portfolio.entity.CommentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface CommentRepository extends JpaRepository<CommentEntity, String> {

    /** All approved comments for a given article, oldest first */
    List<CommentEntity> findByArticleIdAndStatusOrderByCreatedAtAsc(String articleId, String status);

    /** All comments for admin view, newest first */
    List<CommentEntity> findAllByOrderByCreatedAtDesc();

    /** All pending comments — admin use only */
    List<CommentEntity> findByStatusOrderByCreatedAtAsc(String status);

    /** Look up the submitter's own pending comment by their token */
    Optional<CommentEntity> findBySubmitterToken(String submitterToken);
}
