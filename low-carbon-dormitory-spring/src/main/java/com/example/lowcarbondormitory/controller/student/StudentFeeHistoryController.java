package com.example.lowcarbondormitory.controller.student;

import com.example.lowcarbondormitory.common.Result;
import com.example.lowcarbondormitory.dto.response.FeeHistoryPageResponse;
import com.example.lowcarbondormitory.service.student.StudentFeeHistoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/student")
public class StudentFeeHistoryController {

    @Autowired
    private StudentFeeHistoryService studentFeeHistoryService;

    @GetMapping("/fee-history")
    public Result<FeeHistoryPageResponse> getFeeHistory(
            @RequestParam(required = false) String stuNum,
            @RequestParam(required = false) Long dormId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.success(studentFeeHistoryService.getFeeHistory(stuNum, dormId, pageNum, pageSize));
    }
}

