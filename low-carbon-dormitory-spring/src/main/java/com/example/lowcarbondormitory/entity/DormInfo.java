package com.example.lowcarbondormitory.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("student_dorm_info")
public class DormInfo {

    @TableId(value = "dorm_id", type = IdType.AUTO)
    private Long dormId;

    @TableField("dorm_building")
    private String dormBuilding;

    @TableField("dorm_room")
    private String dormRoom;

    @TableField("bed_total")
    private Integer bedTotal;

    @TableField("bed_available")
    private Integer bedAvailable;

    @TableField("dorm_type")
    private String dormType;

    @TableField("dorm_carbon_score")
    private Integer carbonScore;
}
