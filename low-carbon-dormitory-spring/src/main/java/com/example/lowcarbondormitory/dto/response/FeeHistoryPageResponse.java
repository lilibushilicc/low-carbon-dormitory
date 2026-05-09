package com.example.lowcarbondormitory.dto.response;

import com.example.lowcarbondormitory.entity.DormFeeHistory;
import lombok.Data;

import java.util.List;

@Data
public class FeeHistoryPageResponse {
    private List<DormFeeHistory> records;
    private Long total;
    private Long pages;
    private Long current;
}

