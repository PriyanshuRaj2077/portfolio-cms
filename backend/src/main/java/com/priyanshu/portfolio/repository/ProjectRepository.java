package com.priyanshu.portfolio.repository;

import com.priyanshu.portfolio.entity.ProjectEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ProjectRepository extends JpaRepository<ProjectEntity, String> {
    List<ProjectEntity> findAllByOrderBySortOrderAsc();
}
