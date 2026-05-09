package com.example.lowcarbondormitory.dto.response;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class StudentWaterElectricityResponse {

    private String stuNum;
    private Long dormId;
    private String dormNo;
    private String dormBuilding;
    private String dormRoom;
    private BigDecimal electricityBalance;
    private BigDecimal waterBalance;
    private BigDecimal electricityAvailable;
    private BigDecimal waterAvailable;
    private BigDecimal electricityUnitPrice;
    private BigDecimal waterUnitPrice;
    private String electricityUnitName;
    private String waterUnitName;
    private LocalDateTime lastDeductTime;
    private Integer dormCarbonScore;
    private Integer personalCarbonScore;
    private Integer carbonPointsAdded;
    private Integer personalPointsAdded;
}

