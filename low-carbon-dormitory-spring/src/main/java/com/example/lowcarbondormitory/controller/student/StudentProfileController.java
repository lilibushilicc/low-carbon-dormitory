package com.example.lowcarbondormitory.controller.student;

import com.example.lowcarbondormitory.common.Result;
import com.example.lowcarbondormitory.dto.response.StudentProfileResponse;
import com.example.lowcarbondormitory.entity.StudentBase;
import com.example.lowcarbondormitory.service.student.StudentContextService;
import com.example.lowcarbondormitory.service.student.StudentDormService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/student")
public class StudentProfileController {

    @Autowired
    private StudentContextService studentContextService;

    @Autowired
    private StudentDormService studentDormService;

    @GetMapping("/profile")
    public Result<StudentProfileResponse> profile(@RequestParam String stuNum) {
        StudentBase student = studentContextService.getRequiredStudent(stuNum);
        return Result.success(studentDormService.buildStudentProfile(student));
    }
}

