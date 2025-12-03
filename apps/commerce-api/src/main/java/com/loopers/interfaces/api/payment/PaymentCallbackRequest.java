package com.loopers.interfaces.api.payment;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class PaymentCallbackRequest {
    private String transactionKey;
    private String status;
    private String reason;
}
