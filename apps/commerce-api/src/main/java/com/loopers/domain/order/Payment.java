package com.loopers.domain.order;

import com.loopers.domain.BaseEntity;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.*;

/**
 * 결제 정보
 */
@Entity
@Table(
        name = "payments",
        indexes = {
                @Index(name = "idx_order_id", columnList = "order_id"),
                @Index(name = "idx_user_id", columnList = "user_id"),
                @Index(name = "idx_status", columnList = "status"),
                @Index(name = "idx_created_at", columnList = "created_at DESC")
        }
)
public class Payment extends BaseEntity {

    @Column(name = "order_id", nullable = false, unique = true)
    private Long orderId;

    @Column(nullable = false)
    private String userId;

    @Column(nullable = false)
    private Long amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentType paymentType;

    // 결제 상태
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    // PG에서 반환한 거래 고유번호
    @Column(name = "pg_transaction_id")
    private String pgTransactionId;

    // 결제 실패 사유
    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;

    protected Payment() {
    }

    private Payment(Long orderId, String userId, Long amount, PaymentType paymentType) {
        this.orderId = orderId;
        this.userId = userId;
        this.amount = amount;
        this.paymentType = paymentType;
        this.status = PaymentStatus.PENDING;
    }

    /**
     * 결제 생성
     */
    public static Payment create(Long orderId, String userId, Long amount, PaymentType paymentType) {
        validatePaymentInput(amount);
        return new Payment(orderId, userId, amount, paymentType);
    }

    /**
     * PG 거래 ID 업데이트 (PG 요청 직후 저장)
     */
    public void updatePgTransactionId(String pgTransactionId) {
        if (this.pgTransactionId != null) {
            throw new CoreException(
                    ErrorType.BAD_REQUEST,
                    "이미 거래 ID가 설정되어 있습니다: " + this.pgTransactionId
            );
        }
        this.pgTransactionId = pgTransactionId;
    }

    /**
     * 결제 성공 처리
     */
    public void markAsSuccess(String pgTransactionId) {
        if (this.status != PaymentStatus.PENDING) {
            throw new CoreException(
                    ErrorType.BAD_REQUEST,
                    "PENDING 상태에서만 성공 처리할 수 있습니다. 현재 상태: " + this.status
            );
        }

        if (this.pgTransactionId == null) {
            this.pgTransactionId = pgTransactionId;
        } else if (!this.pgTransactionId.equals(pgTransactionId)) {
            throw new CoreException(
                    ErrorType.BAD_REQUEST,
                    "거래 ID가 일치하지 않습니다. expected: " + this.pgTransactionId + ", actual: " + pgTransactionId
            );
        }

        this.status = PaymentStatus.SUCCESS;
    }

    /**
     * 결제 실패 처리
     */
    public void markAsFailed(String reason) {
        if (this.status != PaymentStatus.PENDING) {
            throw new CoreException(
                    ErrorType.BAD_REQUEST,
                    "PENDING 상태에서만 실패 처리할 수 있습니다. 현재 상태: " + this.status
            );
        }
        this.status = PaymentStatus.FAILED;
        this.failureReason = reason;
    }

    private static void validatePaymentInput(Long amount) {
        if (amount == null || amount <= 0) {
            throw new CoreException(
                    ErrorType.BAD_REQUEST,
                    "결제 금액은 0보다 커야 합니다"
            );
        }
    }

    public Long getOrderId() {
        return orderId;
    }

    public String getUserId() {
        return userId;
    }

    public Long getAmount() {
        return amount;
    }

    public PaymentType getPaymentType() {
        return paymentType;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public String getPgTransactionId() {
        return pgTransactionId;
    }

    public String getFailureReason() {
        return failureReason;
    }
}
