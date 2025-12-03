package com.loopers.domain.order;

import com.loopers.infrastructure.persistence.order.PaymentRepository;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PaymentDomainService {

    private final PaymentRepository paymentRepository;

    @Transactional
    public Payment createPayment(Long orderId, String userId, Long amount, PaymentType paymentType) {
        Payment payment = Payment.create(orderId, userId, amount, paymentType);
        return paymentRepository.save(payment);
    }

    @Transactional
    public void updatePgTransactionId(Long paymentId, String pgTransactionId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new CoreException(
                        ErrorType.NOT_FOUND,
                        "결제 정보를 찾을 수 없습니다. paymentId: " + paymentId
                ));
        payment.updatePgTransactionId(pgTransactionId);
        paymentRepository.save(payment);
    }

    @Transactional
    public void markAsSuccess(Long paymentId, String pgTransactionId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new CoreException(
                        ErrorType.NOT_FOUND,
                        "결제 정보를 찾을 수 없습니다. paymentId: " + paymentId
                ));
        payment.markAsSuccess(pgTransactionId);
        paymentRepository.save(payment);
    }

    @Transactional
    public void markAsSuccessByOrderId(Long orderId, String pgTransactionId) {
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new CoreException(
                        ErrorType.NOT_FOUND,
                        "결제 정보를 찾을 수 없습니다. orderId: " + orderId
                ));
        payment.markAsSuccess(pgTransactionId);
        paymentRepository.save(payment);
    }

    @Transactional
    public void markAsFailed(Long paymentId, String reason) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new CoreException(
                        ErrorType.NOT_FOUND,
                        "결제 정보를 찾을 수 없습니다. paymentId: " + paymentId
                ));
        payment.markAsFailed(reason);
        paymentRepository.save(payment);
    }

    @Transactional
    public void markAsFailedByOrderId(Long orderId, String reason) {
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new CoreException(
                        ErrorType.NOT_FOUND,
                        "결제 정보를 찾을 수 없습니다. orderId: " + orderId
                ));
        payment.markAsFailed(reason);
        paymentRepository.save(payment);
    }

    @Transactional(readOnly = true)
    public Payment getPaymentByOrderId(Long orderId) {
        return paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new CoreException(
                        ErrorType.NOT_FOUND,
                        "결제 정보를 찾을 수 없습니다. orderId: " + orderId
                ));
    }

    @Transactional(readOnly = true)
    public List<Payment> getPaymentsByUserId(String userId) {
        return paymentRepository.findByUserId(userId);
    }

    @Transactional(readOnly = true)
    public List<Payment> getPaymentsByUserIdAndStatus(String userId, PaymentStatus status) {
        return paymentRepository.findByUserIdAndStatus(userId, status);
    }
}
