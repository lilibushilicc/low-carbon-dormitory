package com.example.lowcarbondormitory.dto.response;

import lombok.Data;

@Data
public class AdminR2StorageConfigResponse {
    private String endpoint;
    private String accessKeyId;
    private String secretAccessKey;
    private String bucket;
    private String publicBaseUrl;
    private String region;
    private boolean configured;
}
