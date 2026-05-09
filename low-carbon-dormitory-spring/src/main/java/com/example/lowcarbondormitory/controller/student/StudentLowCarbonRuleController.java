package com.example.lowcarbondormitory.controller.student;

import com.example.lowcarbondormitory.common.Result;
import com.example.lowcarbondormitory.dto.response.LowCarbonRuleConfigResponse;
import com.example.lowcarbondormitory.service.rule.LowCarbonRuleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/student/low-carbon-rules")
public class StudentLowCarbonRuleController {

    @Autowired
    private LowCarbonRuleService lowCarbonRuleService;

    @GetMapping
    public Result<LowCarbonRuleConfigResponse> getReadonlyRules() {
        return Result.success(lowCarbonRuleService.getReadonlyConfig());
    }
}

