package com.example.lowcarbondormitory.service.rule;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.lowcarbondormitory.dto.request.LowCarbonRuleConfigRequest;
import com.example.lowcarbondormitory.dto.request.LowCarbonRulePreviewRequest;
import com.example.lowcarbondormitory.dto.response.LowCarbonRuleConfigResponse;
import com.example.lowcarbondormitory.dto.response.LowCarbonRulePreviewResponse;
import com.example.lowcarbondormitory.entity.LowCarbonHonorRule;
import com.example.lowcarbondormitory.entity.LowCarbonScoreRule;
import com.example.lowcarbondormitory.entity.UtilityRateConfig;
import com.example.lowcarbondormitory.mapper.LowCarbonHonorRuleMapper;
import com.example.lowcarbondormitory.mapper.LowCarbonScoreRuleMapper;
import com.example.lowcarbondormitory.mapper.UtilityRateConfigMapper;
import com.example.lowcarbondormitory.service.dashboard.LowCarbonDashboardSupport;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LowCarbonRuleService {

    @Autowired
    private LowCarbonScoreRuleMapper lowCarbonScoreRuleMapper;

    @Autowired
    private LowCarbonHonorRuleMapper lowCarbonHonorRuleMapper;

    @Autowired
    private UtilityRateConfigMapper utilityRateConfigMapper;

    @Autowired
    private LowCarbonDashboardSupport dashboardSupport;

    public LowCarbonDashboardSupport.ScoreRuleSnapshot getActiveScoreRuleSnapshot() {
        LowCarbonScoreRule rule = getActiveScoreRule();
        return new LowCarbonDashboardSupport.ScoreRuleSnapshot(
                defaultIfNull(rule.getBaseScore(), LowCarbonDashboardSupport.BASE_SCORE),
                defaultIfNull(rule.getElectricCarbonFactor(), LowCarbonDashboardSupport.ELECTRIC_CARBON_FACTOR),
                defaultIfNull(rule.getWaterCarbonFactor(), LowCarbonDashboardSupport.WATER_CARBON_FACTOR),
                defaultIfNull(rule.getCarbonPenaltyFactor(), LowCarbonDashboardSupport.CARBON_PENALTY_FACTOR),
                emptyToDefault(rule.getWeeklyDescription(), dashboardSupport.defaultScoreRule().weeklyDescription()),
                emptyToDefault(rule.getMonthlyDescription(), dashboardSupport.defaultScoreRule().monthlyDescription()),
                emptyToDefault(rule.getRankingUpdateNote(), dashboardSupport.defaultScoreRule().rankingUpdateNote())
        );
    }

    public LowCarbonRuleConfigResponse getCurrentConfig() {
        return toConfigResponse(getActiveScoreRule(), listActiveHonorRules());
    }

    public LowCarbonRuleConfigResponse getReadonlyConfig() {
        return getCurrentConfig();
    }

    @Transactional
    public LowCarbonRuleConfigResponse updateConfig(LowCarbonRuleConfigRequest request) {
        lowCarbonScoreRuleMapper.delete(new QueryWrapper<>());
        lowCarbonHonorRuleMapper.delete(new QueryWrapper<>());

        LocalDateTime now = LocalDateTime.now();
        LowCarbonScoreRule scoreRule = new LowCarbonScoreRule();
        scoreRule.setBaseScore(request.getScoreRule().getBaseScore());
        scoreRule.setElectricCarbonFactor(request.getScoreRule().getElectricCarbonFactor());
        scoreRule.setWaterCarbonFactor(request.getScoreRule().getWaterCarbonFactor());
        scoreRule.setCarbonPenaltyFactor(request.getScoreRule().getCarbonPenaltyFactor());
        scoreRule.setWeeklyDescription(request.getScoreRule().getWeeklyDescription());
        scoreRule.setMonthlyDescription(request.getScoreRule().getMonthlyDescription());
        scoreRule.setRankingUpdateNote(request.getScoreRule().getRankingUpdateNote());
        scoreRule.setStatus(1);
        scoreRule.setCreateTime(now);
        scoreRule.setUpdateTime(now);
        lowCarbonScoreRuleMapper.insert(scoreRule);

        List<LowCarbonHonorRule> honorRules = new ArrayList<>();
        for (LowCarbonRuleConfigRequest.HonorRuleForm form : request.getHonorRules()) {
            LowCarbonHonorRule honorRule = new LowCarbonHonorRule();
            honorRule.setPeriodType(form.getPeriodType());
            honorRule.setScopeType(form.getScopeType());
            honorRule.setHonorTitle(form.getHonorTitle());
            honorRule.setBadge(form.getBadge());
            honorRule.setRankType(form.getRankType());
            honorRule.setRankValue(form.getRankValue());
            honorRule.setSortOrder(form.getSortOrder() == null ? 0 : form.getSortOrder());
            honorRule.setStatus(form.getStatus() == null ? 1 : form.getStatus());
            honorRule.setCreateTime(now);
            honorRule.setUpdateTime(now);
            lowCarbonHonorRuleMapper.insert(honorRule);
            honorRules.add(honorRule);
        }
        return toConfigResponse(scoreRule, honorRules);
    }

    public LowCarbonRulePreviewResponse preview(LowCarbonRulePreviewRequest request) {
        LowCarbonDashboardSupport.ScoreRuleSnapshot snapshot = request.getScoreRule() == null
                ? getActiveScoreRuleSnapshot()
                : new LowCarbonDashboardSupport.ScoreRuleSnapshot(
                        request.getScoreRule().getBaseScore(),
                        request.getScoreRule().getElectricCarbonFactor(),
                        request.getScoreRule().getWaterCarbonFactor(),
                        request.getScoreRule().getCarbonPenaltyFactor(),
                        request.getScoreRule().getWeeklyDescription(),
                        request.getScoreRule().getMonthlyDescription(),
                        request.getScoreRule().getRankingUpdateNote()
                );

        UtilityRateConfig electricRate = getRequiredRate("ELECTRIC");
        UtilityRateConfig waterRate = getRequiredRate("WATER");
        LowCarbonDashboardSupport.FeeAggregate aggregate = new LowCarbonDashboardSupport.FeeAggregate();
        aggregate.setElectricFee(request.getElectricFee());
        aggregate.setWaterFee(request.getWaterFee());
        LowCarbonDashboardSupport.MetricSnapshot metric = dashboardSupport.buildMetric(
                aggregate,
                electricRate.getUnitPrice(),
                waterRate.getUnitPrice(),
                snapshot
        );

        LowCarbonRulePreviewResponse response = new LowCarbonRulePreviewResponse();
        response.setElectricFee(metric.electricFee());
        response.setWaterFee(metric.waterFee());
        response.setElectricUsage(metric.electricUsage());
        response.setWaterUsage(metric.waterUsage());
        response.setElectricCarbon(metric.electricCarbon());
        response.setWaterCarbon(metric.waterCarbon());
        response.setTotalCarbon(metric.totalCarbon());
        response.setScore(metric.carbonScore());
        response.setFormulaText(dashboardSupport.buildFormulaText(snapshot));
        response.setHonorPreviewTexts(buildHonorPreviewTexts(listActiveHonorRules()));
        return response;
    }

    public List<LowCarbonHonorRule> listActiveHonorRules() {
        List<LowCarbonHonorRule> rules = lowCarbonHonorRuleMapper.selectList(
                new LambdaQueryWrapper<LowCarbonHonorRule>()
                        .eq(LowCarbonHonorRule::getStatus, 1)
                        .orderByAsc(LowCarbonHonorRule::getPeriodType)
                        .orderByAsc(LowCarbonHonorRule::getScopeType)
                        .orderByAsc(LowCarbonHonorRule::getSortOrder)
                        .orderByAsc(LowCarbonHonorRule::getHonorRuleId)
        );
        if (rules.isEmpty()) {
            return buildDefaultHonorRules();
        }
        return rules;
    }

    private LowCarbonScoreRule getActiveScoreRule() {
        LowCarbonScoreRule rule = lowCarbonScoreRuleMapper.selectOne(
                new LambdaQueryWrapper<LowCarbonScoreRule>()
                        .eq(LowCarbonScoreRule::getStatus, 1)
                        .orderByDesc(LowCarbonScoreRule::getUpdateTime)
                        .last("LIMIT 1")
        );
        if (rule != null) {
            return rule;
        }
        return buildDefaultScoreRule();
    }

    private LowCarbonRuleConfigResponse toConfigResponse(LowCarbonScoreRule scoreRule, List<LowCarbonHonorRule> honorRules) {
        LowCarbonDashboardSupport.ScoreRuleSnapshot snapshot = getSnapshot(scoreRule);
        LowCarbonRuleConfigResponse response = new LowCarbonRuleConfigResponse();

        LowCarbonRuleConfigResponse.ScoreRuleView scoreRuleView = new LowCarbonRuleConfigResponse.ScoreRuleView();
        scoreRuleView.setRuleId(scoreRule.getRuleId());
        scoreRuleView.setBaseScore(snapshot.baseScore());
        scoreRuleView.setElectricCarbonFactor(snapshot.electricCarbonFactor());
        scoreRuleView.setWaterCarbonFactor(snapshot.waterCarbonFactor());
        scoreRuleView.setCarbonPenaltyFactor(snapshot.carbonPenaltyFactor());
        scoreRuleView.setWeeklyDescription(snapshot.weeklyDescription());
        scoreRuleView.setMonthlyDescription(snapshot.monthlyDescription());
        scoreRuleView.setRankingUpdateNote(snapshot.rankingUpdateNote());
        scoreRuleView.setFormulaText(dashboardSupport.buildFormulaText(snapshot));
        scoreRuleView.setUpdatedAt(scoreRule.getUpdateTime());
        response.setScoreRule(scoreRuleView);

        List<LowCarbonRuleConfigResponse.HonorRuleView> honorRuleViews = honorRules.stream()
                .sorted(Comparator.comparing(LowCarbonHonorRule::getPeriodType)
                        .thenComparing(LowCarbonHonorRule::getScopeType)
                        .thenComparing(LowCarbonHonorRule::getSortOrder, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(LowCarbonHonorRule::getHonorRuleId, Comparator.nullsLast(Long::compareTo)))
                .map(this::toHonorRuleView)
                .toList();
        response.setHonorRules(honorRuleViews);
        return response;
    }

    private LowCarbonRuleConfigResponse.HonorRuleView toHonorRuleView(LowCarbonHonorRule rule) {
        LowCarbonRuleConfigResponse.HonorRuleView view = new LowCarbonRuleConfigResponse.HonorRuleView();
        view.setHonorRuleId(rule.getHonorRuleId());
        view.setPeriodType(rule.getPeriodType());
        view.setScopeType(rule.getScopeType());
        view.setHonorTitle(rule.getHonorTitle());
        view.setBadge(rule.getBadge());
        view.setRankType(rule.getRankType());
        view.setRankValue(defaultIfNull(rule.getRankValue(), BigDecimal.ZERO));
        view.setSortOrder(rule.getSortOrder() == null ? 0 : rule.getSortOrder());
        view.setStatus(rule.getStatus() == null ? 1 : rule.getStatus());
        return view;
    }

    private List<String> buildHonorPreviewTexts(List<LowCarbonHonorRule> rules) {
        return rules.stream()
                .sorted(Comparator.comparing(LowCarbonHonorRule::getPeriodType)
                        .thenComparing(LowCarbonHonorRule::getScopeType)
                        .thenComparing(LowCarbonHonorRule::getSortOrder, Comparator.nullsLast(Integer::compareTo)))
                .map(rule -> periodLabel(rule.getPeriodType())
                        + " / " + scopeLabel(rule.getScopeType())
                        + " / " + rule.getHonorTitle()
                        + " / " + resolveThresholdText(rule))
                .toList();
    }

    private String resolveThresholdText(LowCarbonHonorRule rule) {
        if ("RANK".equalsIgnoreCase(rule.getRankType())) {
            return "前 " + defaultIfNull(rule.getRankValue(), BigDecimal.ZERO).stripTrailingZeros().toPlainString() + " 名";
        }
        return "前 " + defaultIfNull(rule.getRankValue(), BigDecimal.ZERO).stripTrailingZeros().toPlainString() + "%";
    }

    public String periodLabel(String periodType) {
        return "monthly".equalsIgnoreCase(periodType) ? "月度" : "周度";
    }

    public String scopeLabel(String scopeType) {
        return switch (scopeType == null ? "" : scopeType.toLowerCase()) {
            case "building" -> "楼栋";
            case "college" -> "学院";
            case "school" -> "全校";
            default -> "未知范围";
        };
    }

    private UtilityRateConfig getRequiredRate(String feeType) {
        UtilityRateConfig rate = utilityRateConfigMapper.selectById(feeType);
        if (rate == null || rate.getUnitPrice() == null || rate.getUnitPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("缺少有效的费率配置: " + feeType);
        }
        return rate;
    }

    private LowCarbonScoreRule buildDefaultScoreRule() {
        LowCarbonDashboardSupport.ScoreRuleSnapshot defaults = dashboardSupport.defaultScoreRule();
        LowCarbonScoreRule rule = new LowCarbonScoreRule();
        rule.setBaseScore(defaults.baseScore());
        rule.setElectricCarbonFactor(defaults.electricCarbonFactor());
        rule.setWaterCarbonFactor(defaults.waterCarbonFactor());
        rule.setCarbonPenaltyFactor(defaults.carbonPenaltyFactor());
        rule.setWeeklyDescription(defaults.weeklyDescription());
        rule.setMonthlyDescription(defaults.monthlyDescription());
        rule.setRankingUpdateNote(defaults.rankingUpdateNote());
        rule.setStatus(1);
        return rule;
    }

    private LowCarbonDashboardSupport.ScoreRuleSnapshot getSnapshot(LowCarbonScoreRule scoreRule) {
        return new LowCarbonDashboardSupport.ScoreRuleSnapshot(
                defaultIfNull(scoreRule.getBaseScore(), LowCarbonDashboardSupport.BASE_SCORE),
                defaultIfNull(scoreRule.getElectricCarbonFactor(), LowCarbonDashboardSupport.ELECTRIC_CARBON_FACTOR),
                defaultIfNull(scoreRule.getWaterCarbonFactor(), LowCarbonDashboardSupport.WATER_CARBON_FACTOR),
                defaultIfNull(scoreRule.getCarbonPenaltyFactor(), LowCarbonDashboardSupport.CARBON_PENALTY_FACTOR),
                emptyToDefault(scoreRule.getWeeklyDescription(), dashboardSupport.defaultScoreRule().weeklyDescription()),
                emptyToDefault(scoreRule.getMonthlyDescription(), dashboardSupport.defaultScoreRule().monthlyDescription()),
                emptyToDefault(scoreRule.getRankingUpdateNote(), dashboardSupport.defaultScoreRule().rankingUpdateNote())
        );
    }

    private List<LowCarbonHonorRule> buildDefaultHonorRules() {
        List<LowCarbonHonorRule> defaults = new ArrayList<>();
        defaults.add(buildDefaultHonorRule("weekly", "building", "楼栋周冠军", "building-top1", "RANK", "1", 10));
        defaults.add(buildDefaultHonorRule("weekly", "building", "楼栋周前10%", "building-top10", "PERCENT", "10", 20));
        defaults.add(buildDefaultHonorRule("weekly", "building", "楼栋周前30%", "building-top30", "PERCENT", "30", 30));
        defaults.add(buildDefaultHonorRule("weekly", "college", "学院周冠军", "college-top1", "RANK", "1", 40));
        defaults.add(buildDefaultHonorRule("weekly", "college", "学院周前10%", "college-top10", "PERCENT", "10", 50));
        defaults.add(buildDefaultHonorRule("weekly", "college", "学院周前30%", "college-top30", "PERCENT", "30", 60));
        defaults.add(buildDefaultHonorRule("weekly", "school", "全校周冠军", "school-top1", "RANK", "1", 70));
        defaults.add(buildDefaultHonorRule("weekly", "school", "全校周前5%", "school-top5", "PERCENT", "5", 80));
        defaults.add(buildDefaultHonorRule("weekly", "school", "全校周前15%", "school-top15", "PERCENT", "15", 90));
        defaults.add(buildDefaultHonorRule("monthly", "building", "楼栋月冠军", "month-building-top1", "RANK", "1", 100));
        defaults.add(buildDefaultHonorRule("monthly", "building", "楼栋月前10%", "month-building-top10", "PERCENT", "10", 110));
        defaults.add(buildDefaultHonorRule("monthly", "building", "楼栋月前30%", "month-building-top30", "PERCENT", "30", 120));
        defaults.add(buildDefaultHonorRule("monthly", "college", "学院月冠军", "month-college-top1", "RANK", "1", 130));
        defaults.add(buildDefaultHonorRule("monthly", "college", "学院月前10%", "month-college-top10", "PERCENT", "10", 140));
        defaults.add(buildDefaultHonorRule("monthly", "college", "学院月前30%", "month-college-top30", "PERCENT", "30", 150));
        defaults.add(buildDefaultHonorRule("monthly", "school", "全校月冠军", "month-school-top1", "RANK", "1", 160));
        defaults.add(buildDefaultHonorRule("monthly", "school", "全校月前5%", "month-school-top5", "PERCENT", "5", 170));
        defaults.add(buildDefaultHonorRule("monthly", "school", "全校月前15%", "month-school-top15", "PERCENT", "15", 180));
        return defaults;
    }

    private LowCarbonHonorRule buildDefaultHonorRule(
            String periodType,
            String scopeType,
            String honorTitle,
            String badge,
            String rankType,
            String rankValue,
            int sortOrder
    ) {
        LowCarbonHonorRule rule = new LowCarbonHonorRule();
        rule.setPeriodType(periodType);
        rule.setScopeType(scopeType);
        rule.setHonorTitle(honorTitle);
        rule.setBadge(badge);
        rule.setRankType(rankType);
        rule.setRankValue(new BigDecimal(rankValue));
        rule.setSortOrder(sortOrder);
        rule.setStatus(1);
        return rule;
    }

    private BigDecimal defaultIfNull(BigDecimal value, BigDecimal defaultValue) {
        return value == null ? defaultValue : value;
    }

    private String emptyToDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }
}
