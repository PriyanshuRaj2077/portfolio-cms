package com.priyanshu.portfolio.repository;

import com.priyanshu.portfolio.entity.ExperienceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ExperienceRepository extends JpaRepository<ExperienceEntity, String> {
    List<ExperienceEntity> findAllByOrderBySortOrderAsc();
}
