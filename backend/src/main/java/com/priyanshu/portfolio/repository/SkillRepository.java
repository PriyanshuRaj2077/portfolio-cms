package com.priyanshu.portfolio.repository;

import com.priyanshu.portfolio.entity.SkillEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SkillRepository extends JpaRepository<SkillEntity, Long> {
    List<SkillEntity> findAllByOrderBySortOrderAsc();
}
