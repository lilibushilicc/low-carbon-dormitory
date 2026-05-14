package com.example.lowcarbondormitory.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import lombok.Data;

@Data
@TableName("system_utility_rate_config")
public class UtilityRateConfig {

    @TableId(value = "fee_type", type = IdType.INPUT)
    private String feeType;

    private BigDecimal unitPrice;

    private String unitName;

    private Boolean enabled;

    public Boolean getEnabled() {
        return enabled == null ? Boolean.TRUE : enabled;
    }

    public boolean isBillingEnabled() {
        return Boolean.TRUE.equals(getEnabled());
    }
}
