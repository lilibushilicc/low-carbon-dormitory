package com.example.lowcarbondormitory.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AdminR2StorageConfigRequest {

    @NotBlank(message = "R2 endpoint 不能为空")
    private String endpoint;

    @NotBlank(message = "Access Key ID 不能为空")
    private String accessKeyId;

    @NotBlank(message = "Secret Access Key 不能为空")
    private String secretAccessKey;

    @NotBlank(message = "Bucket 不能为空")
    private String bucket;

    @NotBlank(message = "公网访问地址不能为空")
    private String publicBaseUrl;

    private String region;
}
