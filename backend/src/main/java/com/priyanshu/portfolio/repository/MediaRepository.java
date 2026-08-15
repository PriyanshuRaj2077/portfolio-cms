package com.priyanshu.portfolio.repository;

import com.priyanshu.portfolio.entity.MediaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MediaRepository extends JpaRepository<MediaEntity, Long> {
}
