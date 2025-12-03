package com.loopers.infrastructure.persistence.order;

import com.loopers.domain.order.Payment;
import com.loopers.domain.order.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 결제 정보를 관리하는 Repository
 */
@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    /**
     * 주문 ID로 결제 조회
     * 주문 1건 = 결제 1건 (unique constraint)
     */
    Optional<Payment> findByOrderId(Long orderId);

    /**
     * 사용자 ID로 결제 목록 조회
     */
    List<Payment> findByUserId(String userId);

    /**
     * 사용자와 상태로 결제 조회
     */
    List<Payment> findByUserIdAndStatus(String userId, PaymentStatus status);

    /**
     * PG 거래 ID로 결제 조회 (콜백 처리용)
     */
    Optional<Payment> findByPgTransactionId(String pgTransactionId);
}
