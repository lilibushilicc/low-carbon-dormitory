package com.example.lowcarbondormitory.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RewardExchangeResult {
    private Long rewardId;
    private String rewardName;
    private Integer spentPoints;
    private Integer remainingPoints;
    private Integer remainingStock;
    private LocalDateTime exchangeTime;
}

