package com.example.lowcarbondormitory.service.student;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.lowcarbondormitory.dto.response.StudentPersonalLowCarbonDashboardResponse;
import com.example.lowcarbondormitory.entity.DormFeeHistory;
import com.example.lowcarbondormitory.entity.DormInfo;
import com.example.lowcarbondormitory.entity.StudentBase;
import com.example.lowcarbondormitory.entity.UtilityRateConfig;
import com.example.lowcarbondormitory.mapper.DormFeeHistoryMapper;
import com.example.lowcarbondormitory.mapper.DormInfoMapper;
import com.example.lowcarbondormitory.mapper.StudentBaseMapper;
import com.example.lowcarbondormitory.mapper.UtilityRateConfigMapper;
import com.example.lowcarbondormitory.service.dashboard.LowCarbonDashboardSupport;
import com.example.lowcarbondormitory.service.rule.LowCarbonRuleService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class StudentPersonalLowCarbonDashboardService {

    @Autowired
    private StudentBaseMapper studentBaseMapper;

    @Autowired
    private DormInfoMapper dormInfoMapper;

    @Autowired
    private DormFeeHistoryMapper dormFeeHistoryMapper;

    @Autowired
    private UtilityRateConfigMapper utilityRateConfigMapper;

    @Autowired
    private StudentDormService studentDormService;

    @Autowired
    private StudentContextService studentContextService;

    @Autowired
    private LowCarbonDashboardSupport dashboardSupport;

    @Autowired
    private LowCarbonRuleService lowCarbonRuleService;

    public StudentPersonalLowCarbonDashboardResponse buildDashboard(String stuNum, Long requestedDormId) {
        return buildDashboard(stuNum, requestedDormId, "weekly");
    }

    public StudentPersonalLowCarbonDashboardResponse buildDashboard(String honorPeriod, String stuNum, Long requestedDormId) {
        return buildDashboard(stuNum, requestedDormId, honorPeriod);
    }

    public StudentPersonalLowCarbonDashboardResponse buildDashboard(String stuNum, Long requestedDormId, String ignoredPeriod) {
        StudentBase currentStudent = studentContextService.getRequiredStudent(stuNum);
        Long dormId = studentContextService.resolveRequiredDormId(currentStudent, requestedDormId);
        DormInfo currentDormInfo = studentContextService.getRequiredDormInfo(dormId);

        List<StudentBase> allStudents = listStudents();
        List<DormInfo> allDorms = listDorms();
        Map<Long, List<StudentBase>> residentsByDorm = groupResidentsByDorm(allStudents);
        String currentCollege = resolveCollege(residentsByDorm.get(dormId));

        UtilityRateConfig electricRate = getRequiredRate("ELECTRIC");
        UtilityRateConfig waterRate = getRequiredRate("WATER");
        LowCarbonDashboardSupport.ScoreRuleSnapshot scoreRule = lowCarbonRuleService.getActiveScoreRuleSnapshot();

        List<DormFeeHistory> allHistories = dormFeeHistoryMapper.selectList(
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
        Map<Long, LowCarbonDashboardSupport.FeeAggregate> latestAggregateMap = dashboardSupport.aggregateLatestRechargeCycles(allHistories);
        LowCarbonDashboardSupport.PeriodWindow currentRankingWindow = dashboardSupport.buildCurrentWindow("weekly");
        Map<Long, LowCarbonDashboardSupport.FeeAggregate> currentPeriodAggregateMap = dashboardSupport.aggregateRechargeCyclesForWindow(
                allHistories,
                currentRankingWindow.rangeStart(),
                currentRankingWindow.rangeEnd()
        );
        Map<Long, LocalDateTime> latestUpdatedAtByDorm = resolveLastUpdatedAtByDorm(allHistories, null, null);
        Map<Long, LocalDateTime> currentPeriodUpdatedAtByDorm = resolveLastUpdatedAtByDorm(
                allHistories,
                currentRankingWindow.rangeStart(),
                currentRankingWindow.rangeEnd()
        );

        List<DormMetrics> metrics = buildDormMetrics(
                allDorms,
                residentsByDorm,
                latestAggregateMap,
                electricRate.getUnitPrice(),
                waterRate.getUnitPrice(),
                electricRate.isBillingEnabled(),
                waterRate.isBillingEnabled(),
                scoreRule,
                latestUpdatedAtByDorm,
                currentPeriodAggregateMap,
                currentPeriodUpdatedAtByDorm
        );

        DormMetrics currentMetric = findCurrentMetrics(metrics, dormId);
        List<DormMetrics> buildingMetrics = filterByBuilding(metrics, currentDormInfo.getDormBuilding());
        List<DormMetrics> collegeMetrics = filterByDormIds(metrics, resolveCollegeDormIds(allStudents, currentCollege));

        StudentPersonalLowCarbonDashboardResponse response = new StudentPersonalLowCarbonDashboardResponse();
        response.setPeriodLabel("当前总览");
        response.setLastUpdatedAt(resolveLastUpdatedAt(allHistories));
        response.setScoreSummary(buildScoreSummary(currentStudent, metrics, dormId, currentMetric));
        response.setRuleSummary(buildRuleSummary(scoreRule));
        response.setDormAnchor(buildDormAnchor(currentDormInfo, currentStudent, residentsByDorm.get(dormId), currentMetric, currentCollege));
        response.setOverview(buildOverview(currentMetric));
        response.setComparisons(buildComparisons(currentMetric, currentDormInfo, buildingMetrics, collegeMetrics, metrics));
        response.setRecommendations(buildRecommendations(currentMetric, buildingMetrics, collegeMetrics, metrics));
        response.setTrends(buildTrends(
                dormId,
                currentDormInfo,
                currentCollege,
                allDorms,
                residentsByDorm,
                electricRate.getUnitPrice(),
                waterRate.getUnitPrice(),
                electricRate.isBillingEnabled(),
                waterRate.isBillingEnabled(),
                scoreRule,
                metrics
        ));
        return response;
    }

    private StudentPersonalLowCarbonDashboardResponse.ScoreSummary buildScoreSummary(
            StudentBase currentStudent,
            List<DormMetrics> metrics,
            Long dormId,
            DormMetrics currentDormMetrics
    ) {
        StudentPersonalLowCarbonDashboardResponse.ScoreSummary summary = new StudentPersonalLowCarbonDashboardResponse.ScoreSummary();
        summary.setDormPoints(currentStudent == null || currentStudent.getCarbonScore() == null ? 0 : Math.max(currentStudent.getCarbonScore(), 0));
        summary.setCurrentPeriodScore(dashboardSupport.scale(currentDormMetrics.getCarbonScore(), 2));
        summary.setCurrentSchoolRank(resolveRank(metrics, dormId));
        summary.setRankedSchoolDormCount(countParticipating(metrics));
        summary.setCurrentPeriodParticipating(currentDormMetrics.isCurrentPeriodParticipating());
        summary.setScoreDelta(BigDecimal.ZERO);
        return summary;
    }

    private StudentPersonalLowCarbonDashboardResponse.RuleSummary buildRuleSummary(LowCarbonDashboardSupport.ScoreRuleSnapshot scoreRule) {
        StudentPersonalLowCarbonDashboardResponse.RuleSummary summary = new StudentPersonalLowCarbonDashboardResponse.RuleSummary();
        summary.setBaseScore(scoreRule.baseScore());
        summary.setElectricCarbonFactor(scoreRule.electricCarbonFactor());
        summary.setWaterCarbonFactor(scoreRule.waterCarbonFactor());
        summary.setCarbonPenaltyFactor(scoreRule.carbonPenaltyFactor());
        summary.setRankingUpdateNote(scoreRule.rankingUpdateNote());
        summary.setFormulaText(dashboardSupport.buildFormulaText(scoreRule));
        return summary;
    }

    private StudentPersonalLowCarbonDashboardResponse.DormAnchor buildDormAnchor(
            DormInfo dormInfo,
            StudentBase currentStudent,
            List<StudentBase> residents,
            DormMetrics currentMetrics,
            String college
    ) {
        List<StudentBase> normalizedResidents = normalizeDormResidents(dormInfo, residents, currentStudent);
        StudentPersonalLowCarbonDashboardResponse.DormAnchor anchor = new StudentPersonalLowCarbonDashboardResponse.DormAnchor();
        anchor.setDormLabel(studentDormService.buildDormNo(dormInfo));
        anchor.setBuilding(dormInfo.getDormBuilding());
        anchor.setCollege(college);
        anchor.setResidentCount(resolveResidentCount(dormInfo, residents));
        anchor.setResidents(normalizedResidents.stream().map(StudentBase::getName).collect(Collectors.toList()));
        anchor.setCarbonLevel(resolveCarbonLevel(currentMetrics.getCarbonScore()));
        anchor.setCurrentPeriodParticipating(currentMetrics.isCurrentPeriodParticipating());
        anchor.setDataUpdatedAt(currentMetrics.getDataUpdatedAt());
        return anchor;
    }

    private StudentPersonalLowCarbonDashboardResponse.Overview buildOverview(DormMetrics currentMetrics) {
        StudentPersonalLowCarbonDashboardResponse.Overview overview = new StudentPersonalLowCarbonDashboardResponse.Overview();
        overview.setElectricFee(dashboardSupport.scale(currentMetrics.getElectricFee(), 2));
        overview.setWaterFee(dashboardSupport.scale(currentMetrics.getWaterFee(), 2));
        overview.setTotalFee(dashboardSupport.scale(currentMetrics.getTotalFee(), 2));
        overview.setElectricUsage(dashboardSupport.scale(currentMetrics.getElectricUsage(), 2));
        overview.setWaterUsage(dashboardSupport.scale(currentMetrics.getWaterUsage(), 2));
        overview.setTotalCarbon(dashboardSupport.scale(currentMetrics.getTotalCarbon(), 2));
        overview.setPerCapitaCarbon(
                dashboardSupport.divideSafe(currentMetrics.getTotalCarbon(), BigDecimal.valueOf(Math.max(currentMetrics.getResidentCount(), 1)), 2)
        );
        overview.setCarbonScore(dashboardSupport.scale(currentMetrics.getCarbonScore(), 2));
        return overview;
    }

    private StudentPersonalLowCarbonDashboardResponse.Comparisons buildComparisons(
            DormMetrics currentMetrics,
            DormInfo currentDormInfo,
            List<DormMetrics> buildingMetrics,
            List<DormMetrics> collegeMetrics,
            List<DormMetrics> schoolMetrics
    ) {
        StudentPersonalLowCarbonDashboardResponse.Comparisons comparisons = new StudentPersonalLowCarbonDashboardResponse.Comparisons();
        comparisons.setBuilding(buildComparisonLevel(currentMetrics, buildingMetrics));
        comparisons.setCollege(buildComparisonLevel(currentMetrics, collegeMetrics));
        comparisons.setSchool(buildComparisonLevel(currentMetrics, schoolMetrics));
        return comparisons;
    }

    private StudentPersonalLowCarbonDashboardResponse.ComparisonLevel buildComparisonLevel(
            DormMetrics currentMetrics,
            List<DormMetrics> metrics
    ) {
        List<DormMetrics> rankedMetrics = filterParticipating(metrics);
        StudentPersonalLowCarbonDashboardResponse.ComparisonLevel level = new StudentPersonalLowCarbonDashboardResponse.ComparisonLevel();
        level.setCurrentPeriodParticipating(currentMetrics.isCurrentPeriodParticipating());
        level.setRank(resolveRank(metrics, currentMetrics.getDormId()));
        level.setTotalDormCount(rankedMetrics.size());
        level.setPercentileText(currentMetrics.isCurrentPeriodParticipating()
                ? resolvePercentileDescription(level.getRank(), level.getTotalDormCount())
                : "本周期暂无新数据，暂不参与排名");
        return level;
    }

    private List<StudentPersonalLowCarbonDashboardResponse.Recommendation> buildRecommendations(
            DormMetrics currentMetrics,
            List<DormMetrics> buildingMetrics,
            List<DormMetrics> collegeMetrics,
            List<DormMetrics> schoolMetrics
    ) {
        List<StudentPersonalLowCarbonDashboardResponse.Recommendation> recommendations = new ArrayList<>();

        BigDecimal buildingAvgFee = averageDormValue(buildingMetrics, DormMetrics::getTotalFee);
        if (currentMetrics.getTotalFee().compareTo(buildingAvgFee) > 0) {
            recommendations.add(buildRecommendation("关注宿舍总费用偏高问题", "当前宿舍总费用高于同楼栋平均水平，建议优先排查照明、待机耗电和异常用水。", "HIGH"));
        }

        DormMetrics collegeBest = filterParticipating(collegeMetrics).stream()
                .max(Comparator.comparing(DormMetrics::getCarbonScore).thenComparing(DormMetrics::getTotalCarbon, Comparator.reverseOrder()))
                .orElse(currentMetrics);
        if (collegeBest.getCarbonScore().compareTo(currentMetrics.getCarbonScore()) > 0) {
            recommendations.add(buildRecommendation("对标学院内优秀宿舍", "学院内已有更高分宿舍，可参考其节能习惯和宿舍管理方式。", "MEDIUM"));
        }

        int schoolRank = resolveRank(schoolMetrics, currentMetrics.getDormId());
        int rankedSchoolCount = countParticipating(schoolMetrics);
        if (schoolRank > Math.max(3, rankedSchoolCount / 2)) {
            recommendations.add(buildRecommendation("提升全校排名稳定性", "建议持续跟踪周度和月度趋势，减少高波动用能行为。", "MEDIUM"));
        }

        recommendations.add(buildRecommendation("坚持低碳日常习惯", "保持随手关灯、集中使用高功率设备、控制热水时长等习惯，有助于持续提升表现。", "LOW"));
        return recommendations;
    }

    private StudentPersonalLowCarbonDashboardResponse.Trends buildTrends(
            Long dormId,
            DormInfo currentDormInfo,
            String currentCollege,
            List<DormInfo> allDorms,
            Map<Long, List<StudentBase>> residentsByDorm,
            BigDecimal electricUnitPrice,
            BigDecimal waterUnitPrice,
            boolean electricBillingEnabled,
            boolean waterBillingEnabled,
            LowCarbonDashboardSupport.ScoreRuleSnapshot scoreRule,
            List<DormMetrics> currentMetrics
    ) {
        List<LowCarbonDashboardSupport.PeriodWindow> windows = dashboardSupport.buildTrendWindows("monthly");
        List<StudentPersonalLowCarbonDashboardResponse.TrendPoint> points = new ArrayList<>();

        Set<Long> collegeDormIds = residentsByDorm.entrySet().stream()
                .filter(entry -> entry.getValue().stream().anyMatch(student -> Objects.equals(student.getCollege(), currentCollege)))
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());

        for (LowCarbonDashboardSupport.PeriodWindow window : windows) {
            List<DormMetrics> metrics = buildDormMetricsForWindow(
                    allDorms,
                    residentsByDorm,
                    window,
                    electricUnitPrice,
                    waterUnitPrice,
                    electricBillingEnabled,
                    waterBillingEnabled,
                    scoreRule
            );
            DormMetrics currentMetric = metrics.stream()
                    .filter(item -> Objects.equals(item.getDormId(), dormId))
                    .findFirst()
                    .orElse(null);
            if (currentMetric == null) {
                continue;
            }

            StudentPersonalLowCarbonDashboardResponse.TrendPoint point = new StudentPersonalLowCarbonDashboardResponse.TrendPoint();
            point.setLabel(window.trendLabel());
            point.setCarbonScore(dashboardSupport.scale(currentMetric.getCarbonScore(), 2));
            point.setSchoolRank(resolveRank(metrics, dormId));
            points.add(point);
        }

        StudentPersonalLowCarbonDashboardResponse.Trends trends = new StudentPersonalLowCarbonDashboardResponse.Trends();
        trends.setPoints(points);
        trends.setBuildingTopDorms(buildTopDorms(filterParticipating(filterByBuilding(currentMetrics, currentDormInfo.getDormBuilding()))));
        trends.setCollegeTopDorms(buildTopDorms(filterParticipating(filterByDormIds(currentMetrics, collegeDormIds))));
        return trends;
    }

    private List<StudentPersonalLowCarbonDashboardResponse.SimpleDormBenchmark> buildTopDorms(List<DormMetrics> metrics) {
        List<DormMetrics> topDorms = metrics.stream()
                .filter(DormMetrics::isCurrentPeriodParticipating)
                .sorted(Comparator.comparing(DormMetrics::getCarbonScore).reversed()
                        .thenComparing(DormMetrics::getTotalCarbon)
                        .thenComparing(DormMetrics::getLabel, Comparator.nullsLast(String::compareTo)))
                .limit(3)
                .toList();

        List<StudentPersonalLowCarbonDashboardResponse.SimpleDormBenchmark> items = new ArrayList<>();
        for (int index = 0; index < topDorms.size(); index++) {
            DormMetrics metric = topDorms.get(index);
            StudentPersonalLowCarbonDashboardResponse.SimpleDormBenchmark item = new StudentPersonalLowCarbonDashboardResponse.SimpleDormBenchmark();
            item.setRank(index + 1);
            item.setDormLabel(metric.getLabel());
            item.setTotalCarbon(dashboardSupport.scale(metric.getTotalCarbon(), 2));
            item.setCarbonScore(dashboardSupport.scale(metric.getCarbonScore(), 2));
            items.add(item);
        }
        return items;
    }

    private List<DormMetrics> buildDormMetrics(
            List<DormInfo> allDorms,
            Map<Long, List<StudentBase>> residentsByDorm,
            Map<Long, LowCarbonDashboardSupport.FeeAggregate> aggregateMap,
            BigDecimal electricUnitPrice,
            BigDecimal waterUnitPrice,
            boolean electricBillingEnabled,
            boolean waterBillingEnabled,
            LowCarbonDashboardSupport.ScoreRuleSnapshot scoreRule,
            Map<Long, LocalDateTime> latestUpdatedAtByDorm,
            Map<Long, LowCarbonDashboardSupport.FeeAggregate> currentPeriodAggregateMap,
            Map<Long, LocalDateTime> currentPeriodUpdatedAtByDorm
    ) {
        List<DormMetrics> metrics = new ArrayList<>();
        for (DormInfo dorm : allDorms) {
            LowCarbonDashboardSupport.MetricSnapshot snapshot = dashboardSupport.buildMetric(
                    aggregateMap.getOrDefault(dorm.getDormId(), new LowCarbonDashboardSupport.FeeAggregate()),
                    electricUnitPrice,
                    waterUnitPrice,
                    electricBillingEnabled,
                    waterBillingEnabled,
                    scoreRule
            );
            List<StudentBase> residents = residentsByDorm.getOrDefault(dorm.getDormId(), List.of());
            boolean currentPeriodParticipating = currentPeriodAggregateMap
                    .getOrDefault(dorm.getDormId(), new LowCarbonDashboardSupport.FeeAggregate())
                    .hasAnyData();
            metrics.add(buildDormMetric(
                    dorm,
                    residents,
                    snapshot,
                    latestUpdatedAtByDorm.get(dorm.getDormId()),
                    currentPeriodUpdatedAtByDorm.get(dorm.getDormId()),
                    currentPeriodParticipating
            ));
        }
        return metrics;
    }

    private List<DormMetrics> buildDormMetricsForWindow(
            List<DormInfo> allDorms,
            Map<Long, List<StudentBase>> residentsByDorm,
            LowCarbonDashboardSupport.PeriodWindow window,
            BigDecimal electricUnitPrice,
            BigDecimal waterUnitPrice,
            boolean electricBillingEnabled,
            boolean waterBillingEnabled,
            LowCarbonDashboardSupport.ScoreRuleSnapshot scoreRule
    ) {
        List<DormFeeHistory> histories = dormFeeHistoryMapper.selectList(
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
                        .lt(DormFeeHistory::getCreateTime, window.rangeEnd())
        );
        Map<Long, LowCarbonDashboardSupport.FeeAggregate> aggregateMap = dashboardSupport.aggregateRechargeCyclesForWindow(
                histories,
                window.rangeStart(),
                window.rangeEnd()
        );
        Map<Long, LocalDateTime> updatedAtByDorm = resolveLastUpdatedAtByDorm(histories, window.rangeStart(), window.rangeEnd());
        return buildDormMetrics(
                allDorms,
                residentsByDorm,
                aggregateMap,
                electricUnitPrice,
                waterUnitPrice,
                electricBillingEnabled,
                waterBillingEnabled,
                scoreRule,
                updatedAtByDorm,
                aggregateMap,
                updatedAtByDorm
        );
    }

    private DormMetrics buildDormMetric(
            DormInfo dorm,
            List<StudentBase> residents,
            LowCarbonDashboardSupport.MetricSnapshot snapshot,
            LocalDateTime dataUpdatedAt,
            LocalDateTime currentPeriodUpdatedAt,
            boolean currentPeriodParticipating
    ) {
        DormMetrics metric = new DormMetrics();
        metric.setDormId(dorm.getDormId());
        metric.setBuilding(dorm.getDormBuilding());
        metric.setLabel(studentDormService.buildDormNo(dorm));
        metric.setResidentCount(resolveResidentCount(dorm, residents));
        metric.setCollege(resolveCollege(residents));
        metric.setElectricFee(snapshot.electricFee());
        metric.setWaterFee(snapshot.waterFee());
        metric.setTotalFee(snapshot.totalFee());
        metric.setElectricUsage(snapshot.electricUsage());
        metric.setWaterUsage(snapshot.waterUsage());
        metric.setTotalCarbon(snapshot.totalCarbon());
        metric.setCarbonScore(snapshot.carbonScore());
        metric.setDataUpdatedAt(dataUpdatedAt);
        metric.setCurrentPeriodUpdatedAt(currentPeriodUpdatedAt);
        metric.setCurrentPeriodParticipating(currentPeriodParticipating);
        return metric;
    }

    private List<StudentBase> listStudents() {
        return studentDormService.attachResolvedDormIds(studentBaseMapper.selectList(
                new LambdaQueryWrapper<StudentBase>()
                        .select(
                                StudentBase::getStudentId,
                                StudentBase::getStuNum,
                                StudentBase::getName,
                                StudentBase::getCollege,
                                StudentBase::getCarbonScore,
                                StudentBase::getDormId,
                                StudentBase::getDormBuilding,
                                StudentBase::getDormRoom
                        )
                        .orderByAsc(StudentBase::getStudentId)
        ));
    }

    private List<DormInfo> listDorms() {
        return dormInfoMapper.selectList(
                new LambdaQueryWrapper<DormInfo>()
                        .select(
                                DormInfo::getDormId,
                                DormInfo::getDormBuilding,
                                DormInfo::getDormRoom,
                                DormInfo::getBedTotal
                        )
                        .orderByAsc(DormInfo::getDormBuilding)
                        .orderByAsc(DormInfo::getDormRoom)
        );
    }

    private Map<Long, List<StudentBase>> groupResidentsByDorm(List<StudentBase> allStudents) {
        return allStudents.stream()
                .filter(item -> item.getDormId() != null)
                .collect(Collectors.groupingBy(StudentBase::getDormId));
    }

    private Set<Long> resolveCollegeDormIds(List<StudentBase> allStudents, String college) {
        return allStudents.stream()
                .filter(item -> item.getDormId() != null)
                .filter(item -> college != null && college.equals(item.getCollege()))
                .map(StudentBase::getDormId)
                .collect(Collectors.toSet());
    }

    private List<DormMetrics> filterByBuilding(List<DormMetrics> metrics, String building) {
        return metrics.stream().filter(item -> Objects.equals(item.getBuilding(), building)).toList();
    }

    private List<DormMetrics> filterByDormIds(List<DormMetrics> metrics, Set<Long> dormIds) {
        return metrics.stream().filter(item -> dormIds.contains(item.getDormId())).toList();
    }

    private List<DormMetrics> filterParticipating(List<DormMetrics> metrics) {
        return metrics.stream().filter(DormMetrics::isCurrentPeriodParticipating).toList();
    }

    private int countParticipating(List<DormMetrics> metrics) {
        return (int) metrics.stream().filter(DormMetrics::isCurrentPeriodParticipating).count();
    }

    private DormMetrics findCurrentMetrics(List<DormMetrics> metrics, Long dormId) {
        return metrics.stream()
                .filter(item -> Objects.equals(item.getDormId(), dormId))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("未找到当前宿舍的低碳指标数据"));
    }

    private UtilityRateConfig getRequiredRate(String feeType) {
        UtilityRateConfig rate = utilityRateConfigMapper.selectById(feeType);
        if (rate == null || rate.getUnitPrice() == null || rate.getUnitPrice().compareTo(LowCarbonDashboardSupport.ZERO) <= 0) {
            throw new IllegalStateException("缺少有效费率配置: " + feeType);
        }
        return rate;
    }

    private String resolveCollege(List<StudentBase> residents) {
        if (residents == null) {
            return null;
        }
        return residents.stream()
                .map(StudentBase::getCollege)
                .filter(Objects::nonNull)
                .filter(item -> !item.isBlank())
                .findFirst()
                .orElse(null);
    }

    private List<StudentBase> normalizeDormResidents(DormInfo dormInfo, List<StudentBase> residents, StudentBase currentStudent) {
        if (residents == null || residents.isEmpty()) {
            return new ArrayList<>();
        }
        int residentCount = resolveResidentCount(dormInfo, residents);
        return residents.stream()
                .filter(Objects::nonNull)
                .sorted(Comparator
                        .comparing((StudentBase item) -> currentStudent == null || !Objects.equals(item.getStudentId(), currentStudent.getStudentId()))
                        .thenComparing(StudentBase::getStuNum, Comparator.nullsLast(String::compareTo))
                        .thenComparing(StudentBase::getStudentId, Comparator.nullsLast(Long::compareTo)))
                .limit(Math.max(residentCount, 0))
                .toList();
    }

    private int resolveResidentCount(DormInfo dormInfo, List<StudentBase> residents) {
        int actualCount = residents == null ? 0 : residents.size();
        if (dormInfo == null || dormInfo.getBedTotal() == null || dormInfo.getBedTotal() <= 0) {
            return actualCount;
        }
        return Math.min(actualCount, dormInfo.getBedTotal());
    }

    private int resolveRank(List<DormMetrics> metrics, Long dormId) {
        List<DormMetrics> ranking = metrics.stream()
                .filter(DormMetrics::isCurrentPeriodParticipating)
                .sorted(Comparator.comparing(DormMetrics::getCarbonScore).reversed()
                        .thenComparing(DormMetrics::getTotalCarbon)
                        .thenComparing(DormMetrics::getLabel, Comparator.nullsLast(String::compareTo)))
                .toList();
        for (int index = 0; index < ranking.size(); index++) {
            if (Objects.equals(ranking.get(index).getDormId(), dormId)) {
                return index + 1;
            }
        }
        return 0;
    }

    private BigDecimal averageDormValue(List<DormMetrics> metrics, Function<DormMetrics, BigDecimal> getter) {
        if (metrics.isEmpty()) {
            return LowCarbonDashboardSupport.ZERO;
        }
        return metrics.stream()
                .map(getter)
                .reduce(LowCarbonDashboardSupport.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(metrics.size()), 2, RoundingMode.HALF_UP);
    }

    private String resolvePercentileDescription(Integer rank, Integer total) {
        if (rank == null || total == null || rank <= 0 || total <= 0) {
            return "暂无排名数据";
        }
        BigDecimal betterCount = BigDecimal.valueOf(total - rank);
        BigDecimal percentile = betterCount.multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(total), 1, RoundingMode.HALF_UP);
        return "超过 " + dashboardSupport.scale(percentile, 1) + "% 的宿舍";
    }

    private StudentPersonalLowCarbonDashboardResponse.Recommendation buildRecommendation(String title, String detail, String priority) {
        StudentPersonalLowCarbonDashboardResponse.Recommendation recommendation = new StudentPersonalLowCarbonDashboardResponse.Recommendation();
        recommendation.setTitle(title);
        recommendation.setDetail(detail);
        recommendation.setPriority(priority);
        return recommendation;
    }

    private String resolveCarbonLevel(BigDecimal carbonScore) {
        if (carbonScore.compareTo(new BigDecimal("90")) >= 0) {
            return "优秀";
        }
        if (carbonScore.compareTo(new BigDecimal("75")) >= 0) {
            return "良好";
        }
        if (carbonScore.compareTo(new BigDecimal("60")) >= 0) {
            return "达标";
        }
        return "待提升";
    }

    private LocalDateTime resolveLastUpdatedAt(List<DormFeeHistory> histories) {
        return histories.stream()
                .map(DormFeeHistory::getCreateTime)
                .filter(Objects::nonNull)
                .max(LocalDateTime::compareTo)
                .orElse(LocalDateTime.now());
    }

    private Map<Long, LocalDateTime> resolveLastUpdatedAtByDorm(
            List<DormFeeHistory> histories,
            LocalDateTime rangeStart,
            LocalDateTime rangeEnd
    ) {
        Map<Long, LocalDateTime> updatedAtByDorm = new LinkedHashMap<>();
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
            updatedAtByDorm.merge(history.getDormId(), history.getCreateTime(), (left, right) -> left.isAfter(right) ? left : right);
        }
        return updatedAtByDorm;
    }

    private static class DormMetrics {
        private Long dormId;
        private String building;
        private String label;
        private String college;
        private Integer residentCount;
        private BigDecimal electricFee = BigDecimal.ZERO;
        private BigDecimal waterFee = BigDecimal.ZERO;
        private BigDecimal totalFee = BigDecimal.ZERO;
        private BigDecimal electricUsage = BigDecimal.ZERO;
        private BigDecimal waterUsage = BigDecimal.ZERO;
        private BigDecimal totalCarbon = BigDecimal.ZERO;
        private BigDecimal carbonScore = BigDecimal.ZERO;
        private boolean currentPeriodParticipating;
        private LocalDateTime dataUpdatedAt;
        private LocalDateTime currentPeriodUpdatedAt;

        public Long getDormId() { return dormId; }
        public void setDormId(Long dormId) { this.dormId = dormId; }
        public String getBuilding() { return building; }
        public void setBuilding(String building) { this.building = building; }
        public String getLabel() { return label; }
        public void setLabel(String label) { this.label = label; }
        public String getCollege() { return college; }
        public void setCollege(String college) { this.college = college; }
        public Integer getResidentCount() { return residentCount == null ? 0 : residentCount; }
        public void setResidentCount(Integer residentCount) { this.residentCount = residentCount; }
        public BigDecimal getElectricFee() { return electricFee; }
        public void setElectricFee(BigDecimal electricFee) { this.electricFee = electricFee; }
        public BigDecimal getWaterFee() { return waterFee; }
        public void setWaterFee(BigDecimal waterFee) { this.waterFee = waterFee; }
        public BigDecimal getTotalFee() { return totalFee; }
        public void setTotalFee(BigDecimal totalFee) { this.totalFee = totalFee; }
        public BigDecimal getElectricUsage() { return electricUsage; }
        public void setElectricUsage(BigDecimal electricUsage) { this.electricUsage = electricUsage; }
        public BigDecimal getWaterUsage() { return waterUsage; }
        public void setWaterUsage(BigDecimal waterUsage) { this.waterUsage = waterUsage; }
        public BigDecimal getTotalCarbon() { return totalCarbon; }
        public void setTotalCarbon(BigDecimal totalCarbon) { this.totalCarbon = totalCarbon; }
        public BigDecimal getCarbonScore() { return carbonScore; }
        public void setCarbonScore(BigDecimal carbonScore) { this.carbonScore = carbonScore; }
        public boolean isCurrentPeriodParticipating() { return currentPeriodParticipating; }
        public void setCurrentPeriodParticipating(boolean currentPeriodParticipating) { this.currentPeriodParticipating = currentPeriodParticipating; }
        public LocalDateTime getDataUpdatedAt() { return dataUpdatedAt; }
        public void setDataUpdatedAt(LocalDateTime dataUpdatedAt) { this.dataUpdatedAt = dataUpdatedAt; }
        public LocalDateTime getCurrentPeriodUpdatedAt() { return currentPeriodUpdatedAt; }
        public void setCurrentPeriodUpdatedAt(LocalDateTime currentPeriodUpdatedAt) { this.currentPeriodUpdatedAt = currentPeriodUpdatedAt; }
    }
}
