package com.example.lowcarbondormitory.dto.response;

import lombok.Data;

@Data
public class AdminLoginResponse {
    private String token;
    private Long adminId;
    private String username;
    private String displayName;
}

