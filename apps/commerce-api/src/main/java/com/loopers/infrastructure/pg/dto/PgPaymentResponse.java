package com.loopers.infrastructure.pg.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class PgPaymentResponse {
    private String transactionKey;
    private String status;
    private String reason;
}