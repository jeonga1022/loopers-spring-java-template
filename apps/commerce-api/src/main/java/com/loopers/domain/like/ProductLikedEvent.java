package com.loopers.domain.like;

import org.springframework.context.ApplicationEvent;

public class ProductLikedEvent extends ApplicationEvent {
    private final Long productId;

    public ProductLikedEvent(Object source, Long productId) {
        super(source);
        this.productId = productId;
    }

    public Long getProductId() {
        return productId;
    }
}