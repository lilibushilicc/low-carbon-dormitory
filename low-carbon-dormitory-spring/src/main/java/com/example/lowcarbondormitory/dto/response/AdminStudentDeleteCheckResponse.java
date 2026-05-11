package com.example.lowcarbondormitory.dto.response;

import lombok.Data;

@Data
public class AdminStudentDeleteCheckResponse {

    private Long studentId;
    private String stuNum;
    private boolean deletable;
    private int rewardExchangeCount;
    private int feeHistoryCount;
    private int paymentOrderCount;
    private String reason;
}
