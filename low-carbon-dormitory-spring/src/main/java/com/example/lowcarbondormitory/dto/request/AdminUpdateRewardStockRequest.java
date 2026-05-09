package com.example.lowcarbondormitory.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AdminUpdateRewardStockRequest {

    @NotNull(message = "库存不能为空")
    @Min(value = 0, message = "库存不能小于0")
    private Integer stock;
}
