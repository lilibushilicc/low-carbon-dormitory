package com.example.lowcarbondormitory.controller.student;

import com.example.lowcarbondormitory.common.Result;
import com.example.lowcarbondormitory.dto.response.StudentWaterElectricityResponse;
import com.example.lowcarbondormitory.entity.DormFee;
import com.example.lowcarbondormitory.entity.DormInfo;
import com.example.lowcarbondormitory.entity.StudentBase;
import com.example.lowcarbondormitory.service.student.DormFeeAccountService;
import com.example.lowcarbondormitory.service.student.StudentContextService;
import com.example.lowcarbondormitory.service.student.StudentDormService;
import com.example.lowcarbondormitory.service.student.StudentPaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/student")
public class StudentWaterElectricityController {

    @Autowired
    private StudentContextService studentContextService;

    @Autowired
    private StudentDormService studentDormService;

    @Autowired
    private DormFeeAccountService dormFeeAccountService;

    @Autowired
    private StudentPaymentService studentPaymentService;

    @GetMapping("/water-electricity")
    public Result<StudentWaterElectricityResponse> waterElectricity(
            @RequestParam(required = false) String stuNum,
            @RequestParam(required = false) Long dormId) {
        if ((stuNum == null || stuNum.trim().isEmpty()) && dormId == null) {
            throw new IllegalArgumentException("学号和宿舍ID不能同时为空");
        }

        StudentBase student = studentContextService.findStudent(stuNum);
        if (student == null && dormId == null) {
            throw new IllegalArgumentException("未找到对应学生，请检查学号或传入宿舍ID");
        }

        Long resolvedDormId = studentContextService.resolveRequiredDormId(student, dormId);
        DormFee dormFee = dormFeeAccountService.getOrCreate(resolvedDormId);
        DormInfo dormInfo = studentDormService.getDormInfo(resolvedDormId);
        return Result.success(studentDormService.buildWaterElectricityResponse(student, resolvedDormId, dormFee, dormInfo));
    }

    @PostMapping("/water-electricity/refresh")
    public Result<StudentWaterElectricityResponse> refreshWaterElectricity(
            @RequestParam(required = false) String stuNum,
            @RequestParam(required = false) Long dormId) {
        return Result.success(studentPaymentService.refreshWaterElectricity(stuNum, dormId));
    }
}
