package com.loopers.infrastructure.pg.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class PgTransactionDetail {
    private String transactionKey;
    private String orderId;
    private String cardType;
    private String cardNo;
    private Long amount;
    private String status;
    private String reason;
}