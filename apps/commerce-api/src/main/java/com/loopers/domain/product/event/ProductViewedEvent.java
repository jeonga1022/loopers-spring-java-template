package com.loopers.domain.product.event;

import java.time.LocalDateTime;

public class ProductViewedEvent {

    private final Long productId;
    private final LocalDateTime occurredAt;

    private ProductViewedEvent(Long productId) {
        this.productId = productId;
        this.occurredAt = LocalDateTime.now();
    }

    public static ProductViewedEvent of(Long productId) {
        return new ProductViewedEvent(productId);
    }

    public Long getProductId() {
        return productId;
    }

    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }
}
