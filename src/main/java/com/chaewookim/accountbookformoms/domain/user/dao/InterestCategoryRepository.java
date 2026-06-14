package com.chaewookim.accountbookformoms.domain.user.dao;

import com.chaewookim.accountbookformoms.domain.user.entity.InterestCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InterestCategoryRepository extends JpaRepository<InterestCategory, Long> {

    List<InterestCategory> findByCoopCategoryNameAndIsAlarmEnabledTrue(String coopCategoryName);
}
