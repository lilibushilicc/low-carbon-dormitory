package com.example.lowcarbondormitory.dto.response;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class LowCarbonDashboardResponse {

    private String period;
    private String periodLabel;
    private Long currentDormId;
    private String currentDormLabel;
    private LocalDateTime rangeStart;
    private LocalDateTime rangeEnd;
    private LocalDateTime lastUpdatedAt;
    private BigDecimal electricUnitPrice;
    private BigDecimal waterUnitPrice;
    private BigDecimal electricCarbonFactor;
    private BigDecimal waterCarbonFactor;
    private Overview overview = new Overview();
    private List<BuildingItem> buildings = new ArrayList<>();
    private List<DormItem> dorms = new ArrayList<>();

    @Data
    public static class Overview {
        private Integer dormCount = 0;
        private Integer rankedDormCount = 0;
        private BigDecimal totalElectricUsage = BigDecimal.ZERO;
        private BigDecimal totalWaterUsage = BigDecimal.ZERO;
        private BigDecimal totalCarbon = BigDecimal.ZERO;
        private BigDecimal averageScore = BigDecimal.ZERO;
        private String bestDormLabel;
        private String highestCarbonDormLabel;
    }

    @Data
    public static class BuildingItem {
        private Integer rank = 0;
        private String name;
        private Integer dormCount = 0;
        private Integer rankedDormCount = 0;
        private BigDecimal totalFee = BigDecimal.ZERO;
        private BigDecimal avgFee = BigDecimal.ZERO;
        private BigDecimal totalCarbon = BigDecimal.ZERO;
        private BigDecimal avgScore = BigDecimal.ZERO;
    }

    @Data
    public static class DormItem {
        private Long dormId;
        private String building;
        private String room;
        private String label;
        private Integer residentCount = 0;
        private List<String> residents = new ArrayList<>();
        private Boolean currentDorm = Boolean.FALSE;
        private BigDecimal electricFee = BigDecimal.ZERO;
        private BigDecimal waterFee = BigDecimal.ZERO;
        private BigDecimal totalFee = BigDecimal.ZERO;
        private BigDecimal electricUsage = BigDecimal.ZERO;
        private BigDecimal waterUsage = BigDecimal.ZERO;
        private BigDecimal electricCarbon = BigDecimal.ZERO;
        private BigDecimal waterCarbon = BigDecimal.ZERO;
        private BigDecimal totalCarbon = BigDecimal.ZERO;
        private BigDecimal carbonScore = BigDecimal.ZERO;
        private Integer energyRank = 0;
        private Integer carbonRank = 0;
        private Boolean currentPeriodParticipating = Boolean.FALSE;
        private String currentPeriodStatus = "STALE";
        private LocalDateTime dataUpdatedAt;
        private LocalDateTime currentPeriodUpdatedAt;
    }
}

