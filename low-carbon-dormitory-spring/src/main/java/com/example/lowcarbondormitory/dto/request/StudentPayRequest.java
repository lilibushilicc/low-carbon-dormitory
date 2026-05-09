package com.example.lowcarbondormitory.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.Data;

@Data
public class StudentPayRequest {

    @NotBlank(message = "学号不能为空")
    @JsonAlias({"stu_num", "userId", "username"})
    private String stuNum;

    @JsonAlias({"dorm_id"})
    private Long dormId;

    @NotNull(message = "缴费金额不能为空")
    @DecimalMin(value = "0.01", message = "缴费金额必须大于0")
    private BigDecimal amount;

    @NotBlank(message = "费用类型不能为空")
    @JsonAlias({"fee_type"})
    private String feeType;

    @JsonAlias({"pay_type"})
    private String payType;

    @JsonAlias({"payerAccount", "payer_account", "payUsername"})
    private String payerAccount;
}
