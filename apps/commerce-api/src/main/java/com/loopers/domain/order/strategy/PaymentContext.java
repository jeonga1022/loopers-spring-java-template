package com.loopers.domain.order.strategy;

public record PaymentContext(
    Long orderId,
    Long paymentId,
    String userId,
    long totalAmount,
    long pointAmount,
    long cardAmount,
    String cardType,
    String cardNo
) {
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long orderId;
        private Long paymentId;
        private String userId;
        private long totalAmount;
        private long pointAmount = 0;
        private long cardAmount = 0;
        private String cardType;
        private String cardNo;

        public Builder orderId(Long orderId) {
            this.orderId = orderId;
            return this;
        }

        public Builder paymentId(Long paymentId) {
            this.paymentId = paymentId;
            return this;
        }

        public Builder userId(String userId) {
            this.userId = userId;
            return this;
        }

        public Builder totalAmount(long totalAmount) {
            this.totalAmount = totalAmount;
            return this;
        }

        public Builder pointAmount(long pointAmount) {
            this.pointAmount = pointAmount;
            return this;
        }

        public Builder cardAmount(long cardAmount) {
            this.cardAmount = cardAmount;
            return this;
        }

        public Builder cardType(String cardType) {
            this.cardType = cardType;
            return this;
        }

        public Builder cardNo(String cardNo) {
            this.cardNo = cardNo;
            return this;
        }

        public PaymentContext build() {
            return new PaymentContext(orderId, paymentId, userId, totalAmount, pointAmount, cardAmount, cardType, cardNo);
        }
    }

    public static PaymentContext forPointOnly(Long orderId, Long paymentId, String userId, long totalAmount) {
        return new PaymentContext(orderId, paymentId, userId, totalAmount, totalAmount, 0, null, null);
    }

    public static PaymentContext forCardOnly(Long orderId, Long paymentId, String userId, long totalAmount, String cardType, String cardNo) {
        return new PaymentContext(orderId, paymentId, userId, totalAmount, 0, totalAmount, cardType, cardNo);
    }

    public static PaymentContext forMixed(Long orderId, Long paymentId, String userId, long totalAmount, long pointAmount, long cardAmount, String cardType, String cardNo) {
        return new PaymentContext(orderId, paymentId, userId, totalAmount, pointAmount, cardAmount, cardType, cardNo);
    }
}
