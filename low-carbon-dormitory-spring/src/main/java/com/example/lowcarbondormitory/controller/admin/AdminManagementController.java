package com.example.lowcarbondormitory.controller.admin;

import com.example.lowcarbondormitory.common.Result;
import com.example.lowcarbondormitory.dto.request.AdminCreateRewardRequest;
import com.example.lowcarbondormitory.dto.request.AdminCreateStudentRequest;
import com.example.lowcarbondormitory.dto.request.AdminDeductDormFeeRequest;
import com.example.lowcarbondormitory.dto.request.AdminUpdateRewardStockRequest;
import com.example.lowcarbondormitory.dto.request.AdminUpdateUtilityRateRequest;
import com.example.lowcarbondormitory.dto.response.AdminCreateStudentResponse;
import com.example.lowcarbondormitory.dto.response.AdminStudentDeleteCheckResponse;
import com.example.lowcarbondormitory.dto.response.AdminStudentListItemResponse;
import com.example.lowcarbondormitory.entity.RewardItem;
import com.example.lowcarbondormitory.entity.UtilityRateConfig;
import com.example.lowcarbondormitory.entity.DormFee;
import com.example.lowcarbondormitory.service.admin.AdminManagementService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/admin")
public class AdminManagementController {

    @Autowired
    private AdminManagementService adminManagementService;

    @GetMapping("/utility-rates")
    public Result<List<UtilityRateConfig>> listUtilityRates() {
        return Result.success(adminManagementService.listUtilityRates());
    }

    @PutMapping("/utility-rates/{feeType}")
    public Result<UtilityRateConfig> updateUtilityRate(
            @PathVariable String feeType,
            @Valid @RequestBody AdminUpdateUtilityRateRequest request
    ) {
        return Result.success(adminManagementService.updateUtilityRate(feeType, request));
    }

    @PostMapping("/dorms/{dormId}/fees/deduct")
    public Result<DormFee> deductDormFee(
            @PathVariable Long dormId,
            @Valid @RequestBody AdminDeductDormFeeRequest request
    ) {
        return Result.success(adminManagementService.deductDormFee(dormId, request));
    }

    @PostMapping("/students")
    public Result<AdminCreateStudentResponse> createStudent(@Valid @RequestBody AdminCreateStudentRequest request) {
        return Result.success(adminManagementService.createStudent(request));
    }

    @GetMapping("/students")
    public Result<List<AdminStudentListItemResponse>> listStudents() {
        return Result.success(adminManagementService.listStudents());
    }

    @GetMapping("/students/{studentId}/delete-check")
    public Result<AdminStudentDeleteCheckResponse> getStudentDeleteCheck(@PathVariable Long studentId) {
        return Result.success(adminManagementService.getDeleteCheck(studentId));
    }

    @DeleteMapping("/students/{studentId}")
    public Result<Boolean> deleteStudent(@PathVariable Long studentId) {
        adminManagementService.deleteStudent(studentId);
        return Result.success(true);
    }

    @GetMapping("/rewards")
    public Result<List<RewardItem>> listRewards() {
        return Result.success(adminManagementService.listRewards());
    }

    @PostMapping("/rewards")
    public Result<RewardItem> createReward(@Valid @RequestBody AdminCreateRewardRequest request) {
        return Result.success(adminManagementService.createReward(request));
    }

    @PutMapping("/rewards/{rewardId}/stock")
    public Result<RewardItem> updateRewardStock(
            @PathVariable Long rewardId,
            @Valid @RequestBody AdminUpdateRewardStockRequest request
    ) {
        return Result.success(adminManagementService.updateRewardStock(rewardId, request));
    }

    @DeleteMapping("/rewards/{rewardId}")
    public Result<Boolean> deleteReward(@PathVariable Long rewardId) {
        adminManagementService.deleteReward(rewardId);
        return Result.success(true);
    }
}


