package com.example.lowcarbondormitory.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RewardExchangeRequest {

    @NotBlank(message = "瀛﹀彿涓嶈兘涓虹┖")
    private String stuNum;

    @NotNull(message = "濂栧搧ID涓嶈兘涓虹┖")
    private Long rewardId;
}

