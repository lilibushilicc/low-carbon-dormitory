package com.example.lowcarbondormitory.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;

@Data
@TableName("student_profile")
public class StudentExt {

    @TableId(value = "ext_id", type = IdType.AUTO)
    private Long extId;

    @TableField("student_id")
    private Long studentId;

    private String avatar;

    private String signature;

    @TableField("admission_date")
    private LocalDate admissionDate;
}
