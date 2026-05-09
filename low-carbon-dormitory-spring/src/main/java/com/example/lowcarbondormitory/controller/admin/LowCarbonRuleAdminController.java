package com.example.lowcarbondormitory.controller.admin;

import com.example.lowcarbondormitory.common.Result;
import com.example.lowcarbondormitory.dto.request.LowCarbonRuleConfigRequest;
import com.example.lowcarbondormitory.dto.request.LowCarbonRulePreviewRequest;
import com.example.lowcarbondormitory.dto.response.LowCarbonRuleConfigResponse;
import com.example.lowcarbondormitory.dto.response.LowCarbonRulePreviewResponse;
import com.example.lowcarbondormitory.service.rule.LowCarbonRuleService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/low-carbon-rules")
public class LowCarbonRuleAdminController {

    @Autowired
    private LowCarbonRuleService lowCarbonRuleService;

    @GetMapping
    public Result<LowCarbonRuleConfigResponse> getConfig() {
        return Result.success(lowCarbonRuleService.getCurrentConfig());
    }

    @PutMapping
    public Result<LowCarbonRuleConfigResponse> updateConfig(@Valid @RequestBody LowCarbonRuleConfigRequest request) {
        return Result.success(lowCarbonRuleService.updateConfig(request));
    }

    @PostMapping("/preview")
    public Result<LowCarbonRulePreviewResponse> preview(@Valid @RequestBody LowCarbonRulePreviewRequest request) {
        return Result.success(lowCarbonRuleService.preview(request));
    }
}


