package com.example.lowcarbondormitory.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("student_fee_history")
public class DormFeeHistory {

    @TableId(value = "history_id", type = IdType.AUTO)
    private Long id;

    @TableField("student_id")
    private Long studentId;

    @TableField("dorm_id")
    private Long dormId;

    @TableField("fee_type")
    private String feeType;

    @TableField("operation_type")
    private String operationType;

    @TableField("pay_type")
    private String payType;

    private BigDecimal amount;

    @TableField("balance_after")
    private BigDecimal balanceAfter;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("payer_name")
    private String payerName;

    @TableField("payer_stu_num")
    private String payerStuNum;

    @TableField("payer_account")
    private String payerAccount;
}


