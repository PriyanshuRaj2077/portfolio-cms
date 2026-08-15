package com.priyanshu.portfolio.repository;

import com.priyanshu.portfolio.entity.SectionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SectionRepository extends JpaRepository<SectionEntity, String> {
    List<SectionEntity> findAllByOrderByOrderAsc();
}
