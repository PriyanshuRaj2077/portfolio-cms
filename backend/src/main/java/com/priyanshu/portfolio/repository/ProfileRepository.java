package com.priyanshu.portfolio.repository;

import com.priyanshu.portfolio.entity.ProfileEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProfileRepository extends JpaRepository<ProfileEntity, Long> {
}
