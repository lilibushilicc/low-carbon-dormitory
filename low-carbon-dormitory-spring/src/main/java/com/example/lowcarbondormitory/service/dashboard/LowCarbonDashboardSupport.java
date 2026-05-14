package com.example.lowcarbondormitory.service.dashboard;

import com.example.lowcarbondormitory.entity.DormFeeHistory;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class LowCarbonDashboardSupport {

    public static final BigDecimal ELECTRIC_CARBON_FACTOR = new BigDecimal("0.785");
    public static final BigDecimal WATER_CARBON_FACTOR = new BigDecimal("0.91");
    public static final BigDecimal BASE_SCORE = new BigDecimal("100");
    public static final BigDecimal CARBON_PENALTY_FACTOR = BigDecimal.ONE;
    public static final BigDecimal ZERO = BigDecimal.ZERO;
    public static final int WEEKLY_WINDOW_DAYS = 7;
    public static final int MONTHLY_WINDOW_DAYS = 30;

    public PeriodWindow buildCurrentWindow(String period) {
        LocalDateTime now = LocalDateTime.now();
        if (isMonthly(period)) {
            LocalDate startDate = LocalDate.now().minusDays(MONTHLY_WINDOW_DAYS - 1L);
            return new PeriodWindow("monthly", "近30天碳排表现", LocalDateTime.of(startDate, LocalTime.MIN), now.plusSeconds(1), "近30天");
        }

        LocalDate startDate = LocalDate.now().minusDays(WEEKLY_WINDOW_DAYS - 1L);
        return new PeriodWindow("weekly", "近7天碳排表现", LocalDateTime.of(startDate, LocalTime.MIN), now.plusSeconds(1), "近7天");
    }

    public List<PeriodWindow> buildTrendWindows(String period) {
        boolean monthly = isMonthly(period);
        int days = monthly ? MONTHLY_WINDOW_DAYS : WEEKLY_WINDOW_DAYS;
        String key = monthly ? "monthly" : "weekly";
        String label = monthly ? "近30天碳排表现" : "近7天碳排表现";

        List<PeriodWindow> windows = new ArrayList<>();
        for (int offset = 3; offset >= 0; offset--) {
            LocalDate endDate = LocalDate.now().minusDays((long) offset * days);
            LocalDate startDate = endDate.minusDays(days - 1L);
            windows.add(new PeriodWindow(
                    key,
                    label,
                    LocalDateTime.of(startDate, LocalTime.MIN),
                    LocalDateTime.of(endDate.plusDays(1), LocalTime.MIN),
                    startDate.format(DateTimeFormatter.ofPattern("M/d")) + "-" + endDate.format(DateTimeFormatter.ofPattern("M/d"))
            ));
        }
        return windows;
    }

    public Map<Long, FeeAggregate> aggregateHistories(List<DormFeeHistory> histories) {
        Map<Long, FeeAggregate> aggregateMap = new LinkedHashMap<>();
        for (DormFeeHistory history : histories) {
            if (history.getDormId() == null || history.getAmount() == null || !isConsumeRecord(history)) {
                continue;
            }

            FeeAggregate aggregate = aggregateMap.computeIfAbsent(history.getDormId(), key -> new FeeAggregate());
            BigDecimal amount = history.getAmount().abs();
            if (isElectricType(history.getFeeType())) {
                aggregate.electricFee = aggregate.electricFee.add(amount);
            } else if (isWaterType(history.getFeeType())) {
                aggregate.waterFee = aggregate.waterFee.add(amount);
            }
        }
        return aggregateMap;
    }

    public Map<Long, FeeAggregate> aggregateLatestRechargeCycles(List<DormFeeHistory> histories) {
        if (histories == null || histories.isEmpty()) {
            return new LinkedHashMap<>();
        }

        Map<Long, List<DormFeeHistory>> groupedRechargeHistories = histories.stream()
                .filter(Objects::nonNull)
                .filter(history -> history.getDormId() != null)
                .filter(this::isRechargeRecord)
                .filter(history -> history.getAmount() != null)
                .filter(history -> history.getCreateTime() != null)
                .collect(Collectors.groupingBy(DormFeeHistory::getDormId, LinkedHashMap::new, Collectors.toList()));

        Map<Long, FeeAggregate> aggregateMap = new LinkedHashMap<>();
        for (Map.Entry<Long, List<DormFeeHistory>> dormEntry : groupedRechargeHistories.entrySet()) {
            FeeAggregate aggregate = new FeeAggregate();
            Map<String, List<DormFeeHistory>> historiesByFeeType = dormEntry.getValue().stream()
                    .collect(Collectors.groupingBy(
                            history -> normalizeFeeType(history.getFeeType()),
                            LinkedHashMap::new,
                            Collectors.toList()
                    ));

            applyLatestRechargeCycle(aggregate, historiesByFeeType.get("ELECTRIC"), true);
            applyLatestRechargeCycle(aggregate, historiesByFeeType.get("WATER"), false);
            aggregateMap.put(dormEntry.getKey(), aggregate);
        }
        return aggregateMap;
    }

    public Map<Long, FeeAggregate> aggregateRechargeCyclesForWindow(
            List<DormFeeHistory> histories,
            LocalDateTime rangeStart,
            LocalDateTime rangeEnd
    ) {
        if (histories == null || histories.isEmpty() || rangeStart == null || rangeEnd == null) {
            return new LinkedHashMap<>();
        }

        Map<Long, List<DormFeeHistory>> groupedRechargeHistories = histories.stream()
                .filter(Objects::nonNull)
                .filter(history -> history.getDormId() != null)
                .filter(this::isRechargeRecord)
                .filter(history -> history.getAmount() != null)
                .filter(history -> history.getCreateTime() != null)
                .collect(Collectors.groupingBy(DormFeeHistory::getDormId, LinkedHashMap::new, Collectors.toList()));

        Map<Long, FeeAggregate> aggregateMap = new LinkedHashMap<>();
        for (Map.Entry<Long, List<DormFeeHistory>> dormEntry : groupedRechargeHistories.entrySet()) {
            FeeAggregate aggregate = new FeeAggregate();
            Map<String, List<DormFeeHistory>> historiesByFeeType = dormEntry.getValue().stream()
                    .collect(Collectors.groupingBy(
                            history -> normalizeFeeType(history.getFeeType()),
                            LinkedHashMap::new,
                            Collectors.toList()
                    ));

            applyRechargeCycleForWindow(aggregate, historiesByFeeType.get("ELECTRIC"), true, rangeStart, rangeEnd);
            applyRechargeCycleForWindow(aggregate, historiesByFeeType.get("WATER"), false, rangeStart, rangeEnd);
            aggregateMap.put(dormEntry.getKey(), aggregate);
        }
        return aggregateMap;
    }

    public MetricSnapshot buildMetric(FeeAggregate aggregate, BigDecimal electricUnitPrice, BigDecimal waterUnitPrice) {
        return buildMetric(aggregate, electricUnitPrice, waterUnitPrice, defaultScoreRule());
    }

    public MetricSnapshot buildMetric(
            FeeAggregate aggregate,
            BigDecimal electricUnitPrice,
            BigDecimal waterUnitPrice,
            ScoreRuleSnapshot rule
    ) {
        return buildMetric(aggregate, electricUnitPrice, waterUnitPrice, true, true, rule);
    }

    public MetricSnapshot buildMetric(
            FeeAggregate aggregate,
            BigDecimal electricUnitPrice,
            BigDecimal waterUnitPrice,
            boolean electricEnabled,
            boolean waterEnabled,
            ScoreRuleSnapshot rule
    ) {
        ScoreRuleSnapshot activeRule = rule == null ? defaultScoreRule() : rule;
        FeeAggregate safeAggregate = aggregate == null ? new FeeAggregate() : aggregate;
        BigDecimal electricFee = electricEnabled ? scale(safeAggregate.electricFee, 2) : ZERO;
        BigDecimal waterFee = waterEnabled ? scale(safeAggregate.waterFee, 2) : ZERO;
        BigDecimal totalFee = electricFee.add(waterFee);
        BigDecimal electricUsage = electricEnabled ? divideSafe(electricFee, electricUnitPrice, 4) : ZERO;
        BigDecimal waterUsage = waterEnabled ? divideSafe(waterFee, waterUnitPrice, 4) : ZERO;
        BigDecimal electricCarbon = electricEnabled ? scale(electricUsage.multiply(activeRule.electricCarbonFactor()), 2) : ZERO;
        BigDecimal waterCarbon = waterEnabled ? scale(waterUsage.multiply(activeRule.waterCarbonFactor()), 2) : ZERO;
        BigDecimal totalCarbon = electricCarbon.add(waterCarbon);
        boolean hasMetricInput = (electricEnabled && safeAggregate.isElectricDataAvailable())
                || (waterEnabled && safeAggregate.isWaterDataAvailable())
                || totalFee.compareTo(ZERO) > 0;
        BigDecimal carbonScore = hasMetricInput
                ? clamp(
                        activeRule.baseScore().subtract(totalCarbon.multiply(activeRule.carbonPenaltyFactor())),
                        ZERO,
                        activeRule.baseScore()
                )
                : ZERO;
        return new MetricSnapshot(
                electricFee,
                waterFee,
                totalFee,
                electricUsage,
                waterUsage,
                electricCarbon,
                waterCarbon,
                totalCarbon,
                scale(carbonScore, 2)
        );
    }

    public ScoreRuleSnapshot defaultScoreRule() {
        return new ScoreRuleSnapshot(
                BASE_SCORE,
                ELECTRIC_CARBON_FACTOR,
                WATER_CARBON_FACTOR,
                CARBON_PENALTY_FACTOR,
                "周维度按最近7天的水电费用估算碳排，并从基础分中扣减对应碳排值。",
                "月维度按最近30天的水电费用估算碳排，并从基础分中扣减对应碳排值。",
                "排名会随最新缴费与扣费记录自动更新。"
        );
    }

    public String buildFormulaText(ScoreRuleSnapshot rule) {
        return "碳积分 = 基础分 - (电费折算用电量*电碳因子 + 水费折算用水量*水碳因子)";
    }

    public boolean isMonthly(String period) {
        if (period == null || period.trim().isEmpty()) {
            return false;
        }
        return "monthly".equals(period.trim().toLowerCase(Locale.ROOT));
    }

    public boolean isConsumeRecord(DormFeeHistory history) {
        String operationType = normalizeText(history.getOperationType());
        if (operationType.contains("RECHARGE")) {
            return false;
        }
        if (operationType.contains("CONSUME")
                || operationType.contains("DEDUCT")
                || operationType.contains("EXPENSE")) {
            return true;
        }
        return history.getAmount() != null && history.getAmount().compareTo(ZERO) < 0;
    }

    public boolean isRechargeRecord(DormFeeHistory history) {
        String operationType = normalizeText(history.getOperationType());
        return operationType.contains("RECHARGE");
    }

    public BigDecimal divideSafe(BigDecimal dividend, BigDecimal divisor, int scale) {
        if (dividend == null || divisor == null || divisor.compareTo(ZERO) <= 0) {
            return ZERO;
        }
        return dividend.divide(divisor, scale, RoundingMode.HALF_UP);
    }

    public BigDecimal scale(BigDecimal value, int scale) {
        return (value == null ? ZERO : value).setScale(scale, RoundingMode.HALF_UP);
    }

    public String formatSigned(BigDecimal value) {
        BigDecimal scaled = scale(value.abs(), 2);
        return scaled.stripTrailingZeros().toPlainString();
    }

    private BigDecimal clamp(BigDecimal value, BigDecimal min, BigDecimal max) {
        BigDecimal safeValue = value == null ? ZERO : value;
        if (safeValue.compareTo(min) < 0) {
            return min;
        }
        if (safeValue.compareTo(max) > 0) {
            return max;
        }
        return safeValue;
    }

    private boolean isElectricType(String feeType) {
        return "ELECTRIC".equals(normalizeFeeType(feeType));
    }

    private boolean isWaterType(String feeType) {
        return "WATER".equals(normalizeFeeType(feeType));
    }

    private String normalizeFeeType(String feeType) {
        String normalized = normalizeText(feeType);
        if (normalized.contains("ELECTRIC")) {
            return "ELECTRIC";
        }
        if (normalized.contains("WATER")) {
            return "WATER";
        }
        return normalized;
    }

    private String normalizeText(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private void applyLatestRechargeCycle(FeeAggregate aggregate, List<DormFeeHistory> histories, boolean electric) {
        if (aggregate == null || histories == null || histories.size() < 2) {
            return;
        }

        List<DormFeeHistory> sortedHistories = histories.stream()
                .filter(Objects::nonNull)
                .sorted(Comparator
                        .comparing(DormFeeHistory::getCreateTime, Comparator.nullsLast(LocalDateTime::compareTo))
                        .thenComparing(DormFeeHistory::getId, Comparator.nullsLast(Long::compareTo))
                        .reversed())
                .toList();
        if (sortedHistories.size() < 2) {
            return;
        }

        DormFeeHistory currentRecharge = sortedHistories.get(0);
        DormFeeHistory lastRecharge = sortedHistories.get(1);
        if (currentRecharge.getBalanceAfter() == null || lastRecharge.getBalanceAfter() == null) {
            return;
        }

        BigDecimal currentBalanceBefore = currentRecharge.getBalanceAfter().subtract(currentRecharge.getAmount());
        BigDecimal consumedAmount = lastRecharge.getBalanceAfter().subtract(currentBalanceBefore);
        if (consumedAmount.compareTo(ZERO) < 0) {
            consumedAmount = ZERO;
        }

        long intervalDays = Math.max(java.time.temporal.ChronoUnit.DAYS.between(lastRecharge.getCreateTime(), currentRecharge.getCreateTime()), 1L);
        BigDecimal periodicFee = consumedAmount.divide(BigDecimal.valueOf(intervalDays), 2, RoundingMode.HALF_UP);
        if (electric) {
            aggregate.setElectricFee(periodicFee);
            aggregate.setElectricDataAvailable(true);
        } else {
            aggregate.setWaterFee(periodicFee);
            aggregate.setWaterDataAvailable(true);
        }
    }

    private void applyRechargeCycleForWindow(
            FeeAggregate aggregate,
            List<DormFeeHistory> histories,
            boolean electric,
            LocalDateTime rangeStart,
            LocalDateTime rangeEnd
    ) {
        if (aggregate == null || histories == null || histories.size() < 2 || rangeStart == null || rangeEnd == null) {
            return;
        }

        List<DormFeeHistory> sortedHistories = histories.stream()
                .filter(Objects::nonNull)
                .sorted(Comparator
                        .comparing(DormFeeHistory::getCreateTime, Comparator.nullsLast(LocalDateTime::compareTo))
                        .thenComparing(DormFeeHistory::getId, Comparator.nullsLast(Long::compareTo))
                        .reversed())
                .toList();

        DormFeeHistory currentRecharge = null;
        DormFeeHistory lastRecharge = null;
        for (int index = 0; index < sortedHistories.size(); index++) {
            DormFeeHistory candidate = sortedHistories.get(index);
            LocalDateTime candidateTime = candidate.getCreateTime();
            if (candidateTime == null || candidateTime.isBefore(rangeStart) || !candidateTime.isBefore(rangeEnd)) {
                continue;
            }
            if (index + 1 >= sortedHistories.size()) {
                break;
            }
            currentRecharge = candidate;
            lastRecharge = sortedHistories.get(index + 1);
            break;
        }

        if (currentRecharge == null || lastRecharge == null) {
            return;
        }
        if (currentRecharge.getBalanceAfter() == null || lastRecharge.getBalanceAfter() == null || currentRecharge.getAmount() == null) {
            return;
        }

        BigDecimal currentBalanceBefore = currentRecharge.getBalanceAfter().subtract(currentRecharge.getAmount());
        BigDecimal consumedAmount = lastRecharge.getBalanceAfter().subtract(currentBalanceBefore);
        if (consumedAmount.compareTo(ZERO) < 0) {
            consumedAmount = ZERO;
        }

        long intervalDays = Math.max(java.time.temporal.ChronoUnit.DAYS.between(lastRecharge.getCreateTime(), currentRecharge.getCreateTime()), 1L);
        BigDecimal periodicFee = consumedAmount.divide(BigDecimal.valueOf(intervalDays), 2, RoundingMode.HALF_UP);
        if (electric) {
            aggregate.setElectricFee(periodicFee);
            aggregate.setElectricDataAvailable(true);
        } else {
            aggregate.setWaterFee(periodicFee);
            aggregate.setWaterDataAvailable(true);
        }
    }

    public static class FeeAggregate {
        private BigDecimal electricFee = BigDecimal.ZERO;
        private BigDecimal waterFee = BigDecimal.ZERO;
        private boolean electricDataAvailable;
        private boolean waterDataAvailable;

        public BigDecimal getElectricFee() {
            return electricFee;
        }

        public void setElectricFee(BigDecimal electricFee) {
            this.electricFee = electricFee == null ? BigDecimal.ZERO : electricFee;
        }

        public BigDecimal getWaterFee() {
            return waterFee;
        }

        public void setWaterFee(BigDecimal waterFee) {
            this.waterFee = waterFee == null ? BigDecimal.ZERO : waterFee;
        }

        public boolean isElectricDataAvailable() {
            return electricDataAvailable;
        }

        public void setElectricDataAvailable(boolean electricDataAvailable) {
            this.electricDataAvailable = electricDataAvailable;
        }

        public boolean isWaterDataAvailable() {
            return waterDataAvailable;
        }

        public void setWaterDataAvailable(boolean waterDataAvailable) {
            this.waterDataAvailable = waterDataAvailable;
        }

        public boolean hasAnyData() {
            return electricDataAvailable || waterDataAvailable;
        }
    }

    public record ScoreRuleSnapshot(
            BigDecimal baseScore,
            BigDecimal electricCarbonFactor,
            BigDecimal waterCarbonFactor,
            BigDecimal carbonPenaltyFactor,
            String weeklyDescription,
            String monthlyDescription,
            String rankingUpdateNote
    ) {
    }

    public record MetricSnapshot(
            BigDecimal electricFee,
            BigDecimal waterFee,
            BigDecimal totalFee,
            BigDecimal electricUsage,
            BigDecimal waterUsage,
            BigDecimal electricCarbon,
            BigDecimal waterCarbon,
            BigDecimal totalCarbon,
            BigDecimal carbonScore
    ) {
    }

    public record PeriodWindow(
            String key,
            String label,
            LocalDateTime rangeStart,
            LocalDateTime rangeEnd,
            String trendLabel
    ) {
    }
}
