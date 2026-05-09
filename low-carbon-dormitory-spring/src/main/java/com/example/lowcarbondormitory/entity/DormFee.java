package com.example.lowcarbondormitory.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("student_dorm_fee")
public class DormFee {

    @TableId("dorm_id")
    private Long dormId;

    @TableField("electricity_balance")
    private BigDecimal electricityBalance;

    @TableField("water_balance")
    private BigDecimal waterBalance;

    @TableField("last_deduct_time")
    private LocalDateTime lastDeductTime;
}

