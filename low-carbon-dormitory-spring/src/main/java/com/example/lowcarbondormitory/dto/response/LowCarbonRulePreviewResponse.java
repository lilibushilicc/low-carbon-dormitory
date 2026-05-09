package com.example.lowcarbondormitory.dto.response;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
public class LowCarbonRulePreviewResponse {
    private BigDecimal electricFee = BigDecimal.ZERO;
    private BigDecimal waterFee = BigDecimal.ZERO;
    private BigDecimal electricUsage = BigDecimal.ZERO;
    private BigDecimal waterUsage = BigDecimal.ZERO;
    private BigDecimal electricCarbon = BigDecimal.ZERO;
    private BigDecimal waterCarbon = BigDecimal.ZERO;
    private BigDecimal totalCarbon = BigDecimal.ZERO;
    private BigDecimal score = BigDecimal.ZERO;
    private String formulaText;
    private List<String> honorPreviewTexts = new ArrayList<>();
}

