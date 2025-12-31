package com.loopers.infrastructure.ranking;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface ProductRankMonthlyRepository extends JpaRepository<ProductRankMonthly, Long> {

    List<ProductRankMonthly> findByPeriodStartOrderByRankingAsc(LocalDate periodStart);

    @Modifying
    @Query("DELETE FROM ProductRankMonthly p WHERE p.periodStart = :periodStart")
    void deleteByPeriodStart(@Param("periodStart") LocalDate periodStart);
}
