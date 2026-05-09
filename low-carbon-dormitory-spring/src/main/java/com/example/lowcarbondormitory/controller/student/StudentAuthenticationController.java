package com.example.lowcarbondormitory.controller.student;

import com.example.lowcarbondormitory.common.Result;
import com.example.lowcarbondormitory.dto.request.StudentLoginRequest;
import com.example.lowcarbondormitory.dto.response.StudentLoginResponse;
import com.example.lowcarbondormitory.service.student.StudentAuthenticationService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/student")
public class StudentAuthenticationController {

    @Autowired
    private StudentAuthenticationService studentAuthenticationService;

    @PostMapping("/login")
    public Result<StudentLoginResponse> login(@Valid @RequestBody StudentLoginRequest request) {
        return Result.success(studentAuthenticationService.login(request));
    }
}


