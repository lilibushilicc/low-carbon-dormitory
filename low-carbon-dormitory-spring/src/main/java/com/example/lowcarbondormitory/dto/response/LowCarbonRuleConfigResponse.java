package com.example.lowcarbondormitory.dto.response;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class LowCarbonRuleConfigResponse {

    private ScoreRuleView scoreRule = new ScoreRuleView();
    private List<HonorRuleView> honorRules = new ArrayList<>();

    @Data
    public static class ScoreRuleView {
        private Long ruleId;
        private BigDecimal baseScore = BigDecimal.ZERO;
        private BigDecimal electricCarbonFactor = BigDecimal.ZERO;
        private BigDecimal waterCarbonFactor = BigDecimal.ZERO;
        private BigDecimal carbonPenaltyFactor = BigDecimal.ZERO;
        private String weeklyDescription;
        private String monthlyDescription;
        private String rankingUpdateNote;
        private String formulaText;
        private LocalDateTime updatedAt;
    }

    @Data
    public static class HonorRuleView {
        private Long honorRuleId;
        private String periodType;
        private String scopeType;
        private String honorTitle;
        private String badge;
        private String rankType;
        private BigDecimal rankValue = BigDecimal.ZERO;
        private Integer sortOrder = 0;
        private Integer status = 1;
    }
}

