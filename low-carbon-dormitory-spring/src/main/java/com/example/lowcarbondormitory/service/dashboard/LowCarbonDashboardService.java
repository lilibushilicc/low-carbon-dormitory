package com.example.lowcarbondormitory.service.dashboard;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.lowcarbondormitory.dto.response.LowCarbonDashboardResponse;
import com.example.lowcarbondormitory.entity.DormFeeHistory;
import com.example.lowcarbondormitory.entity.DormInfo;
import com.example.lowcarbondormitory.entity.StudentBase;
import com.example.lowcarbondormitory.entity.UtilityRateConfig;
import com.example.lowcarbondormitory.mapper.DormFeeHistoryMapper;
import com.example.lowcarbondormitory.mapper.DormInfoMapper;
import com.example.lowcarbondormitory.mapper.StudentBaseMapper;
import com.example.lowcarbondormitory.mapper.UtilityRateConfigMapper;
import com.example.lowcarbondormitory.service.rule.LowCarbonRuleService;
import com.example.lowcarbondormitory.service.student.StudentContextService;
import com.example.lowcarbondormitory.service.student.StudentDormService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class LowCarbonDashboardService {

    @Autowired
    private DormInfoMapper dormInfoMapper;

    @Autowired
    private DormFeeHistoryMapper dormFeeHistoryMapper;

    @Autowired
    private UtilityRateConfigMapper utilityRateConfigMapper;

    @Autowired
    private StudentBaseMapper studentBaseMapper;

    @Autowired
    private StudentDormService studentDormService;

    @Autowired
    private StudentContextService studentContextService;

    @Autowired
    private LowCarbonDashboardSupport dashboardSupport;

    @Autowired
    private LowCarbonRuleService lowCarbonRuleService;

    public LowCarbonDashboardResponse buildDashboard(String period, String stuNum, Long requestedDormId) {
        return buildDashboard(stuNum, requestedDormId, dashboardSupport.buildCurrentWindow(period), false);
    }

    public LowCarbonDashboardResponse buildDashboard(String stuNum, Long requestedDormId) {
        return buildDashboard(stuNum, requestedDormId, dashboardSupport.buildCurrentWindow("weekly"), true);
    }

    private LowCarbonDashboardResponse buildDashboard(
            String stuNum,
            Long requestedDormId,
            LowCarbonDashboardSupport.PeriodWindow rankingWindow,
            boolean overallMode
    ) {
        StudentBase student = studentContextService.findStudent(stuNum);
        Long currentDormId = studentDormService.resolveDormId(student, requestedDormId);

        UtilityRateConfig electricRate = getRequiredRate("ELECTRIC");
        UtilityRateConfig waterRate = getRequiredRate("WATER");
        LowCarbonDashboardSupport.ScoreRuleSnapshot scoreRule = lowCarbonRuleService.getActiveScoreRuleSnapshot();
        List<DormInfo> dormInfos = listDorms();
        List<DormFeeHistory> histories = listHistories();
        Map<Long, LowCarbonDashboardSupport.FeeAggregate> latestAggregateMap = dashboardSupport.aggregateLatestRechargeCycles(histories);
        Map<Long, LowCarbonDashboardSupport.FeeAggregate> rankingAggregateMap = dashboardSupport.aggregateRechargeCyclesForWindow(
                histories,
                rankingWindow.rangeStart(),
                rankingWindow.rangeEnd()
        );
        Map<Long, LowCarbonDashboardSupport.FeeAggregate> aggregateMap = overallMode ? latestAggregateMap : rankingAggregateMap;
        Map<Long, LocalDateTime> allDataUpdatedAtMap = resolveLastUpdatedAtByDorm(histories);
        Map<Long, LocalDateTime> rankingUpdatedAtMap = resolveLastUpdatedAtByDorm(
                histories,
                rankingWindow.rangeStart(),
                rankingWindow.rangeEnd()
        );
        Map<Long, List<StudentBase>> residentsByDorm = buildResidentsByDorm(dormInfos);

        List<LowCarbonDashboardResponse.DormItem> dormItems = buildDormItems(
                dormInfos,
                aggregateMap,
                rankingAggregateMap,
                currentDormId,
                electricRate.getUnitPrice(),
                waterRate.getUnitPrice(),
                electricRate.isBillingEnabled(),
                waterRate.isBillingEnabled(),
                scoreRule,
                overallMode ? allDataUpdatedAtMap : rankingUpdatedAtMap,
                rankingUpdatedAtMap,
                residentsByDorm
        );
        fillRanks(dormItems);

        LowCarbonDashboardResponse response = new LowCarbonDashboardResponse();
        response.setPeriod(overallMode ? "overall" : rankingWindow.key());
        response.setPeriodLabel("绱鏁版嵁");
        response.setCurrentDormId(currentDormId);
        if (!overallMode) {
            response.setPeriodLabel(rankingWindow.label());
        }
        response.setCurrentDormLabel(resolveCurrentDormLabel(dormItems, currentDormId));
        response.setRangeStart(overallMode ? resolveRangeStart(histories) : rankingWindow.rangeStart());
        response.setRangeEnd(overallMode ? resolveRangeEnd(histories) : rankingWindow.rangeEnd());
        response.setLastUpdatedAt(resolveLastUpdatedAt(histories));
        response.setElectricUnitPrice(dashboardSupport.scale(electricRate.getUnitPrice(), 4));
        response.setWaterUnitPrice(dashboardSupport.scale(waterRate.getUnitPrice(), 4));
        response.setElectricCarbonFactor(scoreRule.electricCarbonFactor());
        response.setWaterCarbonFactor(scoreRule.waterCarbonFactor());
        response.setDorms(dormItems);
        response.setBuildings(buildBuildingItems(dormItems));
        response.setOverview(buildOverview(dormItems));
        return response;
    }

    private List<DormInfo> listDorms() {
        return dormInfoMapper.selectList(
                new LambdaQueryWrapper<DormInfo>()
                        .select(
                                DormInfo::getDormId,
                                DormInfo::getDormBuilding,
                                DormInfo::getDormRoom
                        )
                        .orderByAsc(DormInfo::getDormBuilding)
                        .orderByAsc(DormInfo::getDormRoom)
        );
    }

    private List<DormFeeHistory> listHistories() {
        return dormFeeHistoryMapper.selectList(
                new LambdaQueryWrapper<DormFeeHistory>()
                        .select(
                                DormFeeHistory::getId,
                                DormFeeHistory::getDormId,
                                DormFeeHistory::getFeeType,
                                DormFeeHistory::getOperationType,
                                DormFeeHistory::getAmount,
                                DormFeeHistory::getBalanceAfter,
                                DormFeeHistory::getCreateTime
                        )
        );
    }

    private UtilityRateConfig getRequiredRate(String feeType) {
        UtilityRateConfig rate = utilityRateConfigMapper.selectById(feeType);
        if (rate == null || rate.getUnitPrice() == null || rate.getUnitPrice().compareTo(LowCarbonDashboardSupport.ZERO) <= 0) {
            throw new IllegalStateException("濠殿喗锚椤曨參寮幖浣哥闁哄洦姘ㄩ獮鍡涙⒑閺夎法肖闁汇倕妫涚槐鎾诲传閸曗晙鏉? " + feeType);
        }
        return rate;
    }

    private Map<Long, List<StudentBase>> buildResidentsByDorm(List<DormInfo> dormInfos) {
        Map<Long, List<StudentBase>> residentsByDorm = new LinkedHashMap<>();
        if (dormInfos == null || dormInfos.isEmpty()) {
            return residentsByDorm;
        }

        dormInfos.forEach(dormInfo -> residentsByDorm.put(dormInfo.getDormId(), new ArrayList<>()));

        List<StudentBase> students = studentDormService.attachResolvedDormIds(
                studentBaseMapper.selectList(
                        new LambdaQueryWrapper<StudentBase>()
                                .select(
                                        StudentBase::getStudentId,
                                        StudentBase::getStuNum,
                                        StudentBase::getName,
                                        StudentBase::getDormId,
                                        StudentBase::getDormBuilding,
                                        StudentBase::getDormRoom
                                )
                                .orderByAsc(StudentBase::getStudentId)
                )
        );

        for (StudentBase student : students) {
            if (student == null || student.getDormId() == null) {
                continue;
            }
            List<StudentBase> residents = residentsByDorm.get(student.getDormId());
            if (residents != null) {
                residents.add(student);
            }
        }

        return residentsByDorm;
    }

    private List<LowCarbonDashboardResponse.DormItem> buildDormItems(
            List<DormInfo> dormInfos,
            Map<Long, LowCarbonDashboardSupport.FeeAggregate> aggregateMap,
            Map<Long, LowCarbonDashboardSupport.FeeAggregate> currentPeriodAggregateMap,
            Long currentDormId,
            BigDecimal electricUnitPrice,
            BigDecimal waterUnitPrice,
            boolean electricBillingEnabled,
            boolean waterBillingEnabled,
            LowCarbonDashboardSupport.ScoreRuleSnapshot scoreRule,
            Map<Long, LocalDateTime> allDataUpdatedAtMap,
            Map<Long, LocalDateTime> currentPeriodUpdatedAtMap,
            Map<Long, List<StudentBase>> residentsByDorm
    ) {
        List<LowCarbonDashboardResponse.DormItem> items = new ArrayList<>();
        for (DormInfo dormInfo : dormInfos) {
            LowCarbonDashboardSupport.FeeAggregate aggregate = aggregateMap.getOrDefault(
                    dormInfo.getDormId(),
                    new LowCarbonDashboardSupport.FeeAggregate()
            );
            LowCarbonDashboardSupport.FeeAggregate currentPeriodAggregate = currentPeriodAggregateMap.getOrDefault(
                    dormInfo.getDormId(),
                    new LowCarbonDashboardSupport.FeeAggregate()
            );
            LowCarbonDashboardSupport.MetricSnapshot metric = dashboardSupport.buildMetric(
                    aggregate,
                    electricUnitPrice,
                    waterUnitPrice,
                    electricBillingEnabled,
                    waterBillingEnabled,
                    scoreRule
            );
            boolean currentPeriodParticipating = currentPeriodAggregate.hasAnyData();

            LowCarbonDashboardResponse.DormItem item = new LowCarbonDashboardResponse.DormItem();
            item.setDormId(dormInfo.getDormId());
            item.setBuilding(dormInfo.getDormBuilding());
            item.setRoom(dormInfo.getDormRoom());
            item.setLabel(studentDormService.buildDormNo(dormInfo));
            List<StudentBase> residents = residentsByDorm.getOrDefault(dormInfo.getDormId(), List.of());
            item.setResidentCount(residents.size());
            item.setResidents(residents.stream()
                    .map(StudentBase::getName)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList()));
            item.setCurrentDorm(Objects.equals(dormInfo.getDormId(), currentDormId));
            item.setElectricFee(metric.electricFee());
            item.setWaterFee(metric.waterFee());
            item.setTotalFee(metric.totalFee());
            item.setElectricUsage(metric.electricUsage());
            item.setWaterUsage(metric.waterUsage());
            item.setElectricCarbon(metric.electricCarbon());
            item.setWaterCarbon(metric.waterCarbon());
            item.setTotalCarbon(metric.totalCarbon());
            item.setCarbonScore(metric.carbonScore());
            item.setCurrentPeriodParticipating(currentPeriodParticipating);
            item.setCurrentPeriodStatus(currentPeriodParticipating ? "CURRENT" : "STALE");
            item.setDataUpdatedAt(allDataUpdatedAtMap.get(dormInfo.getDormId()));
            item.setCurrentPeriodUpdatedAt(currentPeriodUpdatedAtMap.get(dormInfo.getDormId()));
            items.add(item);
        }

        return items.stream()
                .sorted(Comparator.comparing(LowCarbonDashboardResponse.DormItem::getBuilding, Comparator.nullsLast(String::compareTo))
                        .thenComparing(LowCarbonDashboardResponse.DormItem::getRoom, Comparator.nullsLast(String::compareTo)))
                .collect(Collectors.toList());
    }

    private void fillRanks(List<LowCarbonDashboardResponse.DormItem> dormItems) {
        dormItems.forEach(item -> {
            item.setEnergyRank(0);
            item.setCarbonRank(0);
        });

        List<LowCarbonDashboardResponse.DormItem> energySorted = dormItems.stream()
                .sorted(Comparator.comparing((LowCarbonDashboardResponse.DormItem item) -> !Boolean.TRUE.equals(item.getCurrentPeriodParticipating()))
                        .thenComparing(LowCarbonDashboardResponse.DormItem::getTotalFee)
                        .thenComparing(LowCarbonDashboardResponse.DormItem::getLabel, Comparator.nullsLast(String::compareTo)))
                .toList();
        for (int index = 0; index < energySorted.size(); index++) {
            energySorted.get(index).setEnergyRank(index + 1);
        }

        List<LowCarbonDashboardResponse.DormItem> carbonSorted = dormItems.stream()
                .sorted(Comparator.comparing((LowCarbonDashboardResponse.DormItem item) -> !Boolean.TRUE.equals(item.getCurrentPeriodParticipating()))
                        .thenComparing(LowCarbonDashboardResponse.DormItem::getCarbonScore, Comparator.reverseOrder())
                        .thenComparing(LowCarbonDashboardResponse.DormItem::getTotalCarbon)
                        .thenComparing(LowCarbonDashboardResponse.DormItem::getLabel, Comparator.nullsLast(String::compareTo)))
                .toList();
        for (int index = 0; index < carbonSorted.size(); index++) {
            carbonSorted.get(index).setCarbonRank(index + 1);
        }
    }

    private List<LowCarbonDashboardResponse.BuildingItem> buildBuildingItems(List<LowCarbonDashboardResponse.DormItem> dormItems) {
        Map<String, List<LowCarbonDashboardResponse.DormItem>> grouped = dormItems.stream()
                .collect(Collectors.groupingBy(item -> item.getBuilding() == null ? "閺堫亞鐓″Δ鍏肩埀" : item.getBuilding()));

        List<LowCarbonDashboardResponse.BuildingItem> ranking = grouped.entrySet().stream()
                .map(entry -> {
                    List<LowCarbonDashboardResponse.DormItem> items = entry.getValue();
                    List<LowCarbonDashboardResponse.DormItem> rankedItems = items.stream()
                            .filter(item -> Boolean.TRUE.equals(item.getCurrentPeriodParticipating()))
                            .toList();
                    LowCarbonDashboardResponse.BuildingItem buildingItem = new LowCarbonDashboardResponse.BuildingItem();
                    buildingItem.setName(entry.getKey());
                    buildingItem.setDormCount(items.size());
                    buildingItem.setRankedDormCount(rankedItems.size());
                    buildingItem.setTotalFee(dashboardSupport.scale(sumDormValue(items, LowCarbonDashboardResponse.DormItem::getTotalFee), 2));
                    buildingItem.setAvgFee(dashboardSupport.scale(averageDormValue(items, LowCarbonDashboardResponse.DormItem::getTotalFee), 2));
                    buildingItem.setTotalCarbon(dashboardSupport.scale(sumDormValue(items, LowCarbonDashboardResponse.DormItem::getTotalCarbon), 2));
                    buildingItem.setAvgScore(dashboardSupport.scale(averageDormValue(rankedItems, LowCarbonDashboardResponse.DormItem::getCarbonScore), 2));
                    return buildingItem;
                })
                .sorted(Comparator.comparing((LowCarbonDashboardResponse.BuildingItem item) -> item.getRankedDormCount() <= 0)
                        .thenComparing(LowCarbonDashboardResponse.BuildingItem::getAvgScore, Comparator.reverseOrder())
                        .thenComparing(LowCarbonDashboardResponse.BuildingItem::getName, Comparator.nullsLast(String::compareTo)))
                .collect(Collectors.toList());

        for (int index = 0; index < ranking.size(); index++) {
            ranking.get(index).setRank(index + 1);
        }
        return ranking;
    }

    private LowCarbonDashboardResponse.Overview buildOverview(List<LowCarbonDashboardResponse.DormItem> dormItems) {
        List<LowCarbonDashboardResponse.DormItem> rankedDormItems = dormItems.stream()
                .filter(item -> Boolean.TRUE.equals(item.getCurrentPeriodParticipating()))
                .toList();
        List<LowCarbonDashboardResponse.DormItem> bestDormCandidates = rankedDormItems.isEmpty() ? dormItems : rankedDormItems;
        LowCarbonDashboardResponse.Overview overview = new LowCarbonDashboardResponse.Overview();
        overview.setDormCount(dormItems.size());
        overview.setRankedDormCount(rankedDormItems.size());
        overview.setTotalElectricUsage(dashboardSupport.scale(sumDormValue(dormItems, LowCarbonDashboardResponse.DormItem::getElectricUsage), 2));
        overview.setTotalWaterUsage(dashboardSupport.scale(sumDormValue(dormItems, LowCarbonDashboardResponse.DormItem::getWaterUsage), 2));
        overview.setTotalCarbon(dashboardSupport.scale(sumDormValue(dormItems, LowCarbonDashboardResponse.DormItem::getTotalCarbon), 2));
        overview.setAverageScore(dashboardSupport.scale(averageDormValue(rankedDormItems, LowCarbonDashboardResponse.DormItem::getCarbonScore), 2));
        overview.setBestDormLabel(bestDormCandidates.stream()
                .filter(item -> item.getLabel() != null)
                .max(Comparator.comparing(LowCarbonDashboardResponse.DormItem::getCarbonScore)
                        .thenComparing(LowCarbonDashboardResponse.DormItem::getTotalCarbon, Comparator.reverseOrder())
                        .thenComparing(LowCarbonDashboardResponse.DormItem::getLabel))
                .map(LowCarbonDashboardResponse.DormItem::getLabel)
                .orElse(null));
        overview.setHighestCarbonDormLabel(dormItems.stream()
                .filter(item -> item.getLabel() != null)
                .max(Comparator.comparing(LowCarbonDashboardResponse.DormItem::getTotalCarbon)
                        .thenComparing(LowCarbonDashboardResponse.DormItem::getLabel))
                .map(LowCarbonDashboardResponse.DormItem::getLabel)
                .orElse(null));
        return overview;
    }

    private String resolveCurrentDormLabel(List<LowCarbonDashboardResponse.DormItem> dormItems, Long currentDormId) {
        if (currentDormId == null) {
            return null;
        }
        return dormItems.stream()
                .filter(item -> Objects.equals(item.getDormId(), currentDormId))
                .map(LowCarbonDashboardResponse.DormItem::getLabel)
                .findFirst()
                .orElse(null);
    }

    private LocalDateTime resolveLastUpdatedAt(List<DormFeeHistory> histories) {
        return histories.stream()
                .map(DormFeeHistory::getCreateTime)
                .filter(Objects::nonNull)
                .max(LocalDateTime::compareTo)
                .orElse(LocalDateTime.now());
    }

    private LocalDateTime resolveRangeStart(List<DormFeeHistory> histories) {
        return histories.stream()
                .map(DormFeeHistory::getCreateTime)
                .filter(Objects::nonNull)
                .min(LocalDateTime::compareTo)
                .orElse(LocalDateTime.now());
    }

    private LocalDateTime resolveRangeEnd(List<DormFeeHistory> histories) {
        return histories.stream()
                .map(DormFeeHistory::getCreateTime)
                .filter(Objects::nonNull)
                .max(LocalDateTime::compareTo)
                .orElse(LocalDateTime.now());
    }

    private Map<Long, LocalDateTime> resolveLastUpdatedAtByDorm(List<DormFeeHistory> histories) {
        return resolveLastUpdatedAtByDorm(histories, null, null);
    }

    private Map<Long, LocalDateTime> resolveLastUpdatedAtByDorm(
            List<DormFeeHistory> histories,
            LocalDateTime rangeStart,
            LocalDateTime rangeEnd
    ) {
        Map<Long, LocalDateTime> updatedAtMap = new LinkedHashMap<>();
        for (DormFeeHistory history : histories) {
            if (history == null || history.getDormId() == null || history.getCreateTime() == null) {
                continue;
            }
            if (rangeStart != null && history.getCreateTime().isBefore(rangeStart)) {
                continue;
            }
            if (rangeEnd != null && !history.getCreateTime().isBefore(rangeEnd)) {
                continue;
            }
            updatedAtMap.merge(history.getDormId(), history.getCreateTime(), (left, right) -> left.isAfter(right) ? left : right);
        }
        return updatedAtMap;
    }

    private BigDecimal sumDormValue(
            List<LowCarbonDashboardResponse.DormItem> items,
            java.util.function.Function<LowCarbonDashboardResponse.DormItem, BigDecimal> getter
    ) {
        return items.stream()
                .map(getter)
                .reduce(LowCarbonDashboardSupport.ZERO, BigDecimal::add);
    }

    private BigDecimal averageDormValue(
            List<LowCarbonDashboardResponse.DormItem> items,
            java.util.function.Function<LowCarbonDashboardResponse.DormItem, BigDecimal> getter
    ) {
        if (items.isEmpty()) {
            return LowCarbonDashboardSupport.ZERO;
        }
        return sumDormValue(items, getter).divide(BigDecimal.valueOf(items.size()), 2, RoundingMode.HALF_UP);
    }
}

