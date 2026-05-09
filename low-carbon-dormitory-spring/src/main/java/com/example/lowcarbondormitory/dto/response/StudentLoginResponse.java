package com.example.lowcarbondormitory.dto.response;

import lombok.Data;

@Data
public class StudentLoginResponse {
    private String token;
    private String stuNum;
    private String username;
    private StudentProfileResponse studentInfo;
}

