package com.example.lowcarbondormitory.service.student;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.example.lowcarbondormitory.dto.response.RewardCenterResponse;
import com.example.lowcarbondormitory.dto.response.RewardExchangeResult;
import com.example.lowcarbondormitory.entity.DormInfo;
import com.example.lowcarbondormitory.entity.RewardExchangeRecord;
import com.example.lowcarbondormitory.entity.RewardItem;
import com.example.lowcarbondormitory.entity.StudentBase;
import com.example.lowcarbondormitory.mapper.RewardExchangeRecordMapper;
import com.example.lowcarbondormitory.mapper.RewardItemMapper;
import com.example.lowcarbondormitory.mapper.StudentBaseMapper;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RewardService {

    private static final int MAX_RECENT_RECORDS = 10;

    @Autowired
    private StudentContextService studentContextService;

    @Autowired
    private RewardItemMapper rewardItemMapper;

    @Autowired
    private StudentBaseMapper studentBaseMapper;

    @Autowired
    private RewardExchangeRecordMapper rewardExchangeRecordMapper;

    @Autowired
    private StudentDormService studentDormService;

    public RewardCenterResponse getRewardCenter(String stuNum) {
        return supplementPersonalPoints(stuNum);
    }

    public RewardCenterResponse supplementPersonalPoints(String stuNum) {
        StudentContextService.StudentDormContext context = studentContextService.getRequiredStudentDormContext(stuNum, null);
        StudentBase student = context.student();
        int currentPoints = safePoints(student.getCarbonScore());

        List<RewardItem> rewardItems = rewardItemMapper.selectList(
                new LambdaQueryWrapper<RewardItem>()
                        .eq(RewardItem::getStatus, 1)
                        .orderByAsc(RewardItem::getSortOrder)
                        .orderByAsc(RewardItem::getRewardId)
        );
        List<RewardExchangeRecord> recentRecords = rewardExchangeRecordMapper.selectList(
                new LambdaQueryWrapper<RewardExchangeRecord>()
                        .eq(RewardExchangeRecord::getStudentId, student.getStudentId())
                        .orderByDesc(RewardExchangeRecord::getExchangeTime)
                        .last("LIMIT " + MAX_RECENT_RECORDS)
        );

        RewardCenterResponse response = new RewardCenterResponse();
        response.setCurrentPoints(currentPoints);
        response.setExchangeCount(recentRecords.size());
        response.setDormScoreSummary(buildDormScoreSummary(rewardItems, currentPoints));
        response.setRewardItems(rewardItems.stream().map(item -> toRewardItemView(item, currentPoints)).toList());
        response.setExchangeRecords(recentRecords.stream().map(this::toExchangeRecordView).toList());
        return response;
    }

    @Transactional
    public RewardExchangeResult exchange(String stuNum, Long rewardId) {
        if (rewardId == null) {
            throw new IllegalArgumentException("奖品ID不能为空");
        }

        StudentContextService.StudentDormContext context = studentContextService.getRequiredStudentDormContext(stuNum, null);
        StudentBase student = context.student();
        DormInfo dormInfo = context.dormInfo();

        RewardItem rewardItem = getAvailableRewardItem(rewardId);
        int currentPoints = safePoints(student.getCarbonScore());
        validateExchangeEligibility(rewardItem, currentPoints);

        int pointsCost = rewardItem.getPointsCost();
        LocalDateTime exchangeTime = LocalDateTime.now();

        int studentUpdated = studentBaseMapper.update(
                null,
                new LambdaUpdateWrapper<StudentBase>()
                        .eq(StudentBase::getStudentId, student.getStudentId())
                        .ge(StudentBase::getCarbonScore, pointsCost)
                        .setSql("carbon_score = carbon_score - " + pointsCost)
                        .set(StudentBase::getUpdateTime, exchangeTime)
        );
        if (studentUpdated == 0) {
            throw new IllegalArgumentException("积分不足，兑换失败");
        }

        int rewardUpdated = rewardItemMapper.update(
                null,
                new LambdaUpdateWrapper<RewardItem>()
                        .eq(RewardItem::getRewardId, rewardItem.getRewardId())
                        .eq(RewardItem::getStatus, 1)
                        .gt(RewardItem::getStock, 0)
                        .setSql("stock = stock - 1")
                        .set(RewardItem::getUpdateTime, exchangeTime)
        );
        if (rewardUpdated == 0) {
            throw new IllegalArgumentException("奖品库存不足");
        }

        StudentBase refreshedStudent = studentBaseMapper.selectById(student.getStudentId());
        RewardItem refreshedRewardItem = rewardItemMapper.selectById(rewardItem.getRewardId());
        int remainingPoints = safePoints(refreshedStudent == null ? null : refreshedStudent.getCarbonScore());
        int remainingStock = refreshedRewardItem == null || refreshedRewardItem.getStock() == null
                ? 0
                : Math.max(refreshedRewardItem.getStock(), 0);

        RewardExchangeRecord record = buildExchangeRecord(student, dormInfo.getDormId(), rewardItem, exchangeTime);
        rewardExchangeRecordMapper.insert(record);

        RewardExchangeResult result = new RewardExchangeResult();
        result.setRewardId(rewardItem.getRewardId());
        result.setRewardName(rewardItem.getRewardName());
        result.setSpentPoints(rewardItem.getPointsCost());
        result.setRemainingPoints(remainingPoints);
        result.setRemainingStock(remainingStock);
        result.setExchangeTime(exchangeTime);
        return result;
    }

    private RewardItem getAvailableRewardItem(Long rewardId) {
        RewardItem rewardItem = rewardItemMapper.selectById(rewardId);
        if (rewardItem == null || rewardItem.getStatus() == null || rewardItem.getStatus() != 1) {
            throw new IllegalArgumentException("奖品不存在或已下架");
        }
        return rewardItem;
    }

    private void validateExchangeEligibility(RewardItem rewardItem, int currentPoints) {
        if (rewardItem.getStock() == null || rewardItem.getStock() <= 0) {
            throw new IllegalArgumentException("奖品库存不足");
        }
        if (rewardItem.getPointsCost() == null || currentPoints < rewardItem.getPointsCost()) {
            throw new IllegalArgumentException("当前积分不足以兑换该奖品");
        }
    }

    private RewardExchangeRecord buildExchangeRecord(
            StudentBase student,
            Long dormId,
            RewardItem rewardItem,
            LocalDateTime exchangeTime
    ) {
        RewardExchangeRecord record = new RewardExchangeRecord();
        record.setRewardId(rewardItem.getRewardId());
        record.setStudentId(student.getStudentId());
        record.setDormId(dormId);
        record.setRewardName(rewardItem.getRewardName());
        record.setImageUrl(rewardItem.getImageUrl());
        record.setExchangePoints(rewardItem.getPointsCost());
        record.setStatus("SUCCESS");
        record.setRemark("积分兑换成功");
        record.setStudentName(student.getName());
        record.setStuNum(student.getStuNum());
        record.setExchangeTime(exchangeTime);
        return record;
    }

    private RewardCenterResponse.RewardItemView toRewardItemView(RewardItem rewardItem, int currentPoints) {
        RewardCenterResponse.RewardItemView view = new RewardCenterResponse.RewardItemView();
        view.setRewardId(rewardItem.getRewardId());
        view.setRewardName(rewardItem.getRewardName());
        view.setRewardDesc(rewardItem.getRewardDesc());
        view.setPointsCost(rewardItem.getPointsCost());
        view.setImageUrl(rewardItem.getImageUrl());
        view.setStock(rewardItem.getStock());

        boolean hasStock = rewardItem.getStock() != null && rewardItem.getStock() > 0;
        boolean enoughPoints = rewardItem.getPointsCost() != null && currentPoints >= rewardItem.getPointsCost();
        view.setCanExchange(hasStock && enoughPoints);
        view.setExchangeTip(buildExchangeTip(hasStock, enoughPoints));
        return view;
    }

    private RewardCenterResponse.ExchangeRecordView toExchangeRecordView(RewardExchangeRecord record) {
        RewardCenterResponse.ExchangeRecordView view = new RewardCenterResponse.ExchangeRecordView();
        view.setRecordId(record.getRecordId());
        view.setRewardName(record.getRewardName());
        view.setExchangePoints(record.getExchangePoints());
        view.setRemark(record.getRemark());
        view.setExchangeTime(record.getExchangeTime());
        return view;
    }

    private String buildExchangeTip(boolean hasStock, boolean enoughPoints) {
        if (!hasStock) {
            return "库存不足";
        }
        if (!enoughPoints) {
            return "当前积分不足";
        }
        return "可立即兑换";
    }

    private int safePoints(Integer points) {
        return points == null ? 0 : Math.max(points, 0);
    }

    private RewardCenterResponse.DormScoreSummary buildDormScoreSummary(List<RewardItem> rewardItems, int currentPoints) {
        RewardCenterResponse.DormScoreSummary summary = new RewardCenterResponse.DormScoreSummary();
        rewardItems.stream()
                .filter(item -> item.getPointsCost() != null)
                .min(Comparator.comparingInt(RewardItem::getPointsCost))
                .ifPresent(item -> {
                    summary.setNearestRewardName(item.getRewardName());
                    summary.setGapToNearestReward(Math.max(item.getPointsCost() - currentPoints, 0));
                });
        return summary;
    }
}
