package com.loopers.infrastructure.metrics;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProductMetricsTest {

    @Test
    @DisplayName("ProductMetrics 생성 시 모든 카운트는 0이어야 한다")
    void createTest1() {
        ProductMetrics metrics = ProductMetrics.create(1L);

        assertThat(metrics.getProductId()).isEqualTo(1L);
        assertThat(metrics.getLikeCount()).isEqualTo(0L);
        assertThat(metrics.getOrderCount()).isEqualTo(0L);
        assertThat(metrics.getTotalQuantity()).isEqualTo(0L);
    }

    @Test
    @DisplayName("incrementLikeCount 호출 시 좋아요 수가 1 증가한다")
    void incrementLikeCountTest1() {
        ProductMetrics metrics = ProductMetrics.create(1L);

        metrics.incrementLikeCount();

        assertThat(metrics.getLikeCount()).isEqualTo(1L);
    }

    @Test
    @DisplayName("addOrder 호출 시 주문 수와 총 판매 수량이 증가한다")
    void addOrderTest1() {
        ProductMetrics metrics = ProductMetrics.create(1L);

        metrics.addOrder(3);

        assertThat(metrics.getOrderCount()).isEqualTo(1L);
        assertThat(metrics.getTotalQuantity()).isEqualTo(3L);
    }
}
