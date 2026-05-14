package com.example.lowcarbondormitory.dto.response;

import lombok.Data;

@Data
public class AdminRewardImageUploadResponse {
    private String imageUrl;
    private String originalName;
    private long size;
}
