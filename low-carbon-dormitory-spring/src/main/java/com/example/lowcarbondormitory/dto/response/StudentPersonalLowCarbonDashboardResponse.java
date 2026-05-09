package com.example.lowcarbondormitory.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class StudentPersonalLowCarbonDashboardResponse {

    private String periodLabel;
    private LocalDateTime lastUpdatedAt;
    private ScoreSummary scoreSummary = new ScoreSummary();
    private RuleSummary ruleSummary = new RuleSummary();
    private DormAnchor dormAnchor = new DormAnchor();
    private Overview overview = new Overview();
    private Comparisons comparisons = new Comparisons();
    private List<Recommendation> recommendations = new ArrayList<>();
    private Trends trends = new Trends();

    @Data
    public static class ScoreSummary {
        private Integer dormPoints = 0;
        private BigDecimal currentPeriodScore = BigDecimal.ZERO;
        private Integer currentSchoolRank = 0;
        private Integer rankedSchoolDormCount = 0;
        private Boolean currentPeriodParticipating = Boolean.FALSE;
        private BigDecimal scoreDelta = BigDecimal.ZERO;
    }

    @Data
    public static class RuleSummary {
        private BigDecimal baseScore = BigDecimal.ZERO;
        private BigDecimal electricCarbonFactor = BigDecimal.ZERO;
        private BigDecimal waterCarbonFactor = BigDecimal.ZERO;
        private BigDecimal carbonPenaltyFactor = BigDecimal.ZERO;
        private String rankingUpdateNote;
        private String formulaText;
    }

    @Data
    public static class DormAnchor {
        private String dormLabel;
        private String building;
        private String college;
        private Integer residentCount = 0;
        private List<String> residents = new ArrayList<>();
        private String carbonLevel;
        private Boolean currentPeriodParticipating = Boolean.FALSE;
        private LocalDateTime dataUpdatedAt;
    }

    @Data
    public static class Overview {
        private BigDecimal electricFee = BigDecimal.ZERO;
        private BigDecimal waterFee = BigDecimal.ZERO;
        private BigDecimal totalFee = BigDecimal.ZERO;
        private BigDecimal electricUsage = BigDecimal.ZERO;
        private BigDecimal waterUsage = BigDecimal.ZERO;
        private BigDecimal totalCarbon = BigDecimal.ZERO;
        private BigDecimal perCapitaCarbon = BigDecimal.ZERO;
        private BigDecimal carbonScore = BigDecimal.ZERO;
    }

    @Data
    public static class Comparisons {
        private ComparisonLevel building = new ComparisonLevel();
        private ComparisonLevel college = new ComparisonLevel();
        private ComparisonLevel school = new ComparisonLevel();
    }

    @Data
    public static class ComparisonLevel {
        private Integer rank = 0;
        private Integer totalDormCount = 0;
        private Boolean currentPeriodParticipating = Boolean.FALSE;
        private String percentileText;
    }

    @Data
    public static class Recommendation {
        private String title;
        private String detail;
        private String priority;
    }

    @Data
    public static class Trends {
        private List<TrendPoint> points = new ArrayList<>();
        private List<SimpleDormBenchmark> buildingTopDorms = new ArrayList<>();
        private List<SimpleDormBenchmark> collegeTopDorms = new ArrayList<>();
    }

    @Data
    public static class TrendPoint {
        private String label;
        private BigDecimal carbonScore = BigDecimal.ZERO;
        private Integer schoolRank = 0;
    }

    @Data
    public static class SimpleDormBenchmark {
        private String dormLabel;
        private BigDecimal totalCarbon = BigDecimal.ZERO;
        private BigDecimal carbonScore = BigDecimal.ZERO;
        private Integer rank = 0;
    }
}
