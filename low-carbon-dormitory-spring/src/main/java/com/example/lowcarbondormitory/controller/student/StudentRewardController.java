package com.example.lowcarbondormitory.controller.student;

import com.example.lowcarbondormitory.common.Result;
import com.example.lowcarbondormitory.dto.request.RewardExchangeRequest;
import com.example.lowcarbondormitory.dto.response.RewardCenterResponse;
import com.example.lowcarbondormitory.dto.response.RewardExchangeResult;
import com.example.lowcarbondormitory.service.student.RewardService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/student/rewards")
public class StudentRewardController {

    @Autowired
    private RewardService rewardService;

    @GetMapping
    public Result<RewardCenterResponse> rewardCenter(@RequestParam String stuNum) {
        return Result.success(rewardService.getRewardCenter(stuNum));
    }

    @PostMapping("/supplement-points")
    public Result<RewardCenterResponse> supplementPoints(@RequestParam String stuNum) {
        return Result.success(rewardService.supplementPersonalPoints(stuNum));
    }

    @PostMapping("/exchange")
    public Result<RewardExchangeResult> exchange(@Valid @RequestBody RewardExchangeRequest request) {
        return Result.success(rewardService.exchange(request.getStuNum(), request.getRewardId()));
    }
}
