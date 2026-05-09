package com.example.lowcarbondormitory.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

@Data
public class RewardCenterResponse {
    private Integer currentPoints;
    private Integer exchangeCount;
    private DormScoreSummary dormScoreSummary = new DormScoreSummary();
    private List<RewardItemView> rewardItems;
    private List<ExchangeRecordView> exchangeRecords;

    @Data
    public static class DormScoreSummary {
        private String nearestRewardName;
        private Integer gapToNearestReward = 0;
    }

    @Data
    public static class RewardItemView {
        private Long rewardId;
        private String rewardName;
        private String rewardDesc;
        private Integer pointsCost;
        private String imageUrl;
        private Integer stock;
        private Boolean canExchange;
        private String exchangeTip;
    }

    @Data
    public static class ExchangeRecordView {
        private Long recordId;
        private String rewardName;
        private Integer exchangePoints;
        private String remark;
        private LocalDateTime exchangeTime;
    }
}
