package com.loopers.infrastructure.metrics;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ProductMetricsRepository extends JpaRepository<ProductMetrics, Long> {

    Optional<ProductMetrics> findByProductId(Long productId);

    Optional<ProductMetrics> findByProductIdAndDate(Long productId, LocalDate date);

    List<ProductMetrics> findByDateBetween(LocalDate startDate, LocalDate endDate);
}
