package com.example.lowcarbondormitory.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AdminCreateRewardRequest {

    @NotBlank(message = "奖品名称不能为空")
    private String rewardName;

    @NotBlank(message = "奖品描述不能为空")
    private String rewardDesc;

    @NotNull(message = "兑换积分不能为空")
    @Min(value = 0, message = "兑换积分不能小于0")
    private Integer pointsCost;

    private String imageUrl;

    @NotNull(message = "库存不能为空")
    @Min(value = 0, message = "库存不能小于0")
    private Integer stock;

    @Min(value = 0, message = "排序值不能小于0")
    private Integer sortOrder;

    private Integer status;
}
