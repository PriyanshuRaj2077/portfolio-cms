package com.priyanshu.portfolio.repository;

import com.priyanshu.portfolio.entity.AchievementEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AchievementRepository extends JpaRepository<AchievementEntity, String> {
    List<AchievementEntity> findAllByOrderBySortOrderAsc();
}
