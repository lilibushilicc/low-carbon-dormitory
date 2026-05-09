package com.example.lowcarbondormitory.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class StudentLoginRequest {

    @NotBlank(message = "学号不能为空")
    @JsonAlias({"stuNum", "stu_num", "username"})
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;
}
