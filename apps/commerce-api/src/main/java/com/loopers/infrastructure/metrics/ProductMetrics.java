package com.loopers.infrastructure.metrics;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "product_metrics")
public class ProductMetrics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private Long productId;

    private Long likeCount;
    private Long orderCount;
    private Long totalQuantity;

    protected ProductMetrics() {
    }

    private ProductMetrics(Long productId) {
        this.productId = productId;
        this.likeCount = 0L;
        this.orderCount = 0L;
        this.totalQuantity = 0L;
    }

    public static ProductMetrics create(Long productId) {
        return new ProductMetrics(productId);
    }

    public void incrementLikeCount() {
        this.likeCount++;
    }

    public void addOrder(int quantity) {
        this.orderCount++;
        this.totalQuantity += quantity;
    }

    public Long getId() {
        return id;
    }

    public Long getProductId() {
        return productId;
    }

    public Long getLikeCount() {
        return likeCount;
    }

    public Long getOrderCount() {
        return orderCount;
    }

    public Long getTotalQuantity() {
        return totalQuantity;
    }
}
