package com.example.lowcarbondormitory.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("student_base")
public class StudentBase {

    @TableId(value = "student_id", type = IdType.AUTO)
    private Long studentId;

    @TableField("stu_num")
    private String stuNum;

    private String name;
    private Integer gender;

    @TableField("id_card")
    private String idCard;

    private String phone;
    private String college;
    private String major;

    @TableField("class_name")
    private String className;

    private String grade;

    @TableField("dorm_building")
    private String dormBuilding;

    @TableField("dorm_room")
    private String dormRoom;

    @TableField("carbon_score")
    private Integer carbonScore;

    private String password;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;

    @TableField("dorm_id")
    private Long dormId;
}
