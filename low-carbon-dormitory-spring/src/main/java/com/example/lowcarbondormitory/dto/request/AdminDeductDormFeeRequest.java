package com.example.lowcarbondormitory.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.Data;

@Data
public class AdminDeductDormFeeRequest {

    @NotNull(message = "扣费金额不能为空")
    @DecimalMin(value = "0.01", message = "扣费金额必须大于0")
    private BigDecimal amount;

    @NotBlank(message = "费用类型不能为空")
    @JsonAlias({"fee_type"})
    private String feeType;

    @JsonAlias({"pay_type"})
    private String payType;

    @JsonAlias({"payer_name"})
    private String payerName;

    @JsonAlias({"payer_account"})
    private String payerAccount;

    private String remark;
}
