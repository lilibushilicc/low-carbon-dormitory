package com.example.lowcarbondormitory.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("student_low_carbon_score_rule")
public class LowCarbonScoreRule {

    @TableId(value = "rule_id", type = IdType.AUTO)
    private Long ruleId;

    @TableField("base_score")
    private BigDecimal baseScore;

    @TableField("electric_carbon_factor")
    private BigDecimal electricCarbonFactor;

    @TableField("water_carbon_factor")
    private BigDecimal waterCarbonFactor;

    @TableField("carbon_penalty_factor")
    private BigDecimal carbonPenaltyFactor;

    @TableField("weekly_description")
    private String weeklyDescription;

    @TableField("monthly_description")
    private String monthlyDescription;

    @TableField("ranking_update_note")
    private String rankingUpdateNote;

    private Integer status;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;
}



