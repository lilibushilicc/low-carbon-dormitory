package com.example.lowcarbondormitory.controller.student;

import com.example.lowcarbondormitory.common.Result;
import com.example.lowcarbondormitory.dto.response.LowCarbonDashboardResponse;
import com.example.lowcarbondormitory.dto.response.StudentPersonalLowCarbonDashboardResponse;
import com.example.lowcarbondormitory.service.dashboard.LowCarbonDashboardService;
import com.example.lowcarbondormitory.service.student.StudentPersonalLowCarbonDashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/student")
public class StudentLowCarbonDashboardController {

    @Autowired
    private LowCarbonDashboardService lowCarbonDashboardService;

    @Autowired
    private StudentPersonalLowCarbonDashboardService studentPersonalLowCarbonDashboardService;

    @GetMapping("/low-carbon-dashboard")
    public Result<LowCarbonDashboardResponse> getLowCarbonDashboard(
            @RequestParam(required = false) String stuNum,
            @RequestParam(required = false) Long dormId
    ) {
        return Result.success(lowCarbonDashboardService.buildDashboard(stuNum, dormId));
    }

    @GetMapping("/low-carbon-dashboard-personal")
    public Result<StudentPersonalLowCarbonDashboardResponse> getStudentPersonalLowCarbonDashboard(
            @RequestParam(required = false) String stuNum,
            @RequestParam(required = false) Long dormId
    ) {
        return Result.success(studentPersonalLowCarbonDashboardService.buildDashboard(stuNum, dormId));
    }
}

