package com.example.lowcarbondormitory.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
public class LowCarbonRuleConfigRequest {

    @Valid
    @NotNull
    private ScoreRuleForm scoreRule = new ScoreRuleForm();

    @Valid
    @NotEmpty
    private List<HonorRuleForm> honorRules = new ArrayList<>();

    @Data
    public static class ScoreRuleForm {
        @NotNull
        @DecimalMin("0")
        private BigDecimal baseScore;

        @NotNull
        @DecimalMin("0")
        private BigDecimal electricCarbonFactor;

        @NotNull
        @DecimalMin("0")
        private BigDecimal waterCarbonFactor;

        @NotNull
        @DecimalMin("0")
        private BigDecimal carbonPenaltyFactor;

        private String weeklyDescription;
        private String monthlyDescription;
        private String rankingUpdateNote;
    }

    @Data
    public static class HonorRuleForm {
        @NotNull
        private String periodType;

        @NotNull
        private String scopeType;

        @NotNull
        private String honorTitle;

        private String badge;

        @NotNull
        private String rankType;

        @NotNull
        @DecimalMin("0")
        private BigDecimal rankValue;

        private Integer sortOrder = 0;
        private Integer status = 1;
    }
}


