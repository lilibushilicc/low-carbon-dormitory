package com.example.lowcarbondormitory.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.Data;

@Data
public class AdminUpdateUtilityRateRequest {

    @NotNull(message = "单价不能为空")
    @DecimalMin(value = "0.0001", message = "单价必须大于0")
    private BigDecimal unitPrice;

    @NotBlank(message = "单位名称不能为空")
    private String unitName;
}
