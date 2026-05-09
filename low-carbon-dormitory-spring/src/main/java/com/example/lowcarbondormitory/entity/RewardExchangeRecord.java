package com.example.lowcarbondormitory.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("student_reward_exchange")
public class RewardExchangeRecord {

    @TableId(value = "record_id", type = IdType.AUTO)
    private Long recordId;

    @TableField("reward_id")
    private Long rewardId;

    @TableField("student_id")
    private Long studentId;

    @TableField("dorm_id")
    private Long dormId;

    @TableField("reward_name")
    private String rewardName;

    @TableField("image_url")
    private String imageUrl;

    @TableField("exchange_points")
    private Integer exchangePoints;

    private String status;
    private String remark;

    @TableField("student_name")
    private String studentName;

    @TableField("stu_num")
    private String stuNum;

    @TableField("exchange_time")
    private LocalDateTime exchangeTime;
}



