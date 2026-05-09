package com.example.lowcarbondormitory.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class LowCarbonRulePreviewRequest {

    @NotNull
    @DecimalMin("0")
    private BigDecimal electricFee;

    @NotNull
    @DecimalMin("0")
    private BigDecimal waterFee;

    @Valid
    private LowCarbonRuleConfigRequest.ScoreRuleForm scoreRule;
}


