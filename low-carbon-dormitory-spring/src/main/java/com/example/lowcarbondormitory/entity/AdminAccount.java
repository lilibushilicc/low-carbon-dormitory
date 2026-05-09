package com.example.lowcarbondormitory.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("system_admin_account")
public class AdminAccount {

    @TableId(value = "admin_id", type = IdType.AUTO)
    private Long adminId;

    private String username;

    private String password;

    @TableField("display_name")
    private String displayName;

    private Integer status;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;
}

