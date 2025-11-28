package com.loopers.infrastructure.cache;

import com.loopers.domain.like.ProductLikedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class ProductCacheEventListener {
    private final ProductCacheService productCacheService;

    @TransactionalEventListener
    public void onProductLiked(ProductLikedEvent event) {
        productCacheService.invalidateProductListCaches();
    }
}