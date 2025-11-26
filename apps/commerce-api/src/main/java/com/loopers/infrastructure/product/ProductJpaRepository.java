package com.loopers.infrastructure.product;

import com.loopers.domain.product.Product;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ProductJpaRepository extends JpaRepository<Product, Long> {

    Page<Product> findByBrandId(Long brandId, Pageable pageable);

    List<Product> findByBrandId(Long brandId);

    @Query("SELECT p FROM Product p WHERE p.id = :id AND p.deletedAt IS NULL")
    Optional<Product> findByIdAndNotDeleted(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.id = :id")
    Optional<Product> findByIdWithLock(Long id);

    @Modifying
    @Query("UPDATE Product p SET p.totalLikes = p.totalLikes + 1 WHERE p.id = :id")
    void incrementLikeCount(Long id);

    @Modifying
    @Query("UPDATE Product p SET p.totalLikes = p.totalLikes - 1 WHERE p.id = :id AND p.totalLikes > 0")
    void decrementLikeCount(Long id);
}
