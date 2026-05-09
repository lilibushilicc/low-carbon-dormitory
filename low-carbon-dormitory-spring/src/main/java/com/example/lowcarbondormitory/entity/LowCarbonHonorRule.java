package com.example.lowcarbondormitory.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("student_low_carbon_honor_rule")
public class LowCarbonHonorRule {

    @TableId(value = "honor_rule_id", type = IdType.AUTO)
    private Long honorRuleId;

    @TableField("period_type")
    private String periodType;

    @TableField("scope_type")
    private String scopeType;

    @TableField("honor_title")
    private String honorTitle;

    private String badge;

    @TableField("rank_type")
    private String rankType;

    @TableField("rank_value")
    private BigDecimal rankValue;

    @TableField("sort_order")
    private Integer sortOrder;

    private Integer status;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;
}



