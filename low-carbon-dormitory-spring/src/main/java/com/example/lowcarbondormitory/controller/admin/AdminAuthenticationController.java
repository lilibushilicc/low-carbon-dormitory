package com.example.lowcarbondormitory.controller.admin;

import com.example.lowcarbondormitory.common.Result;
import com.example.lowcarbondormitory.dto.request.AdminLoginRequest;
import com.example.lowcarbondormitory.dto.response.AdminLoginResponse;
import com.example.lowcarbondormitory.service.admin.AdminManagementService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin")
public class AdminAuthenticationController {

    @Autowired
    private AdminManagementService adminManagementService;

    @PostMapping("/login")
    public Result<AdminLoginResponse> login(@Valid @RequestBody AdminLoginRequest request) {
        return Result.success(adminManagementService.login(request));
    }
}


