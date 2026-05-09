package com.example.lowcarbondormitory.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("student_reward_item")
public class RewardItem {

    @TableId(value = "reward_id", type = IdType.AUTO)
    private Long rewardId;

    @TableField("reward_name")
    private String rewardName;

    @TableField("reward_desc")
    private String rewardDesc;

    @TableField("points_cost")
    private Integer pointsCost;

    @TableField("image_url")
    private String imageUrl;

    private Integer stock;
    private Integer status;

    @TableField("sort_order")
    private Integer sortOrder;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;
}

