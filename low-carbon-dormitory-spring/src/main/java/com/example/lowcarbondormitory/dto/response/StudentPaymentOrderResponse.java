package com.example.lowcarbondormitory.dto.response;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class StudentPaymentOrderResponse {

    private String orderNo;

    private String status;

    private BigDecimal amount;

    private String feeType;

    private String payType;

    private String qrCodeContent;

    private String qrCodeImageUrl;

    private String thirdTradeNo;

    private LocalDateTime createTime;

    private LocalDateTime paidTime;

    private Integer carbonPointsAdded;

    private Integer personalPointsAdded;
}

