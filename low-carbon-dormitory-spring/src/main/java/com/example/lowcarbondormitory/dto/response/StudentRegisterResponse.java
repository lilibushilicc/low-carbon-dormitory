package com.example.lowcarbondormitory.dto.response;

import lombok.Data;

@Data
public class StudentRegisterResponse {
    private Long studentId;
    private String stuNum;
    private Long dormId;
}
