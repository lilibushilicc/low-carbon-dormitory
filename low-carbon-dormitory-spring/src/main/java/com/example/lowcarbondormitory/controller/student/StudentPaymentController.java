package com.example.lowcarbondormitory.controller.student;

import com.example.lowcarbondormitory.common.Result;
import com.example.lowcarbondormitory.dto.request.StudentPayRequest;
import com.example.lowcarbondormitory.dto.response.StudentPaymentOrderResponse;
import com.example.lowcarbondormitory.dto.response.StudentWaterElectricityResponse;
import com.example.lowcarbondormitory.service.student.StudentPaymentService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/student")
public class StudentPaymentController {

    @Autowired
    private StudentPaymentService studentPaymentService;

    @PostMapping("/pay")
    public Result<StudentWaterElectricityResponse> pay(@Valid @RequestBody StudentPayRequest request) {
        return Result.success(studentPaymentService.pay(request));
    }

    @PostMapping("/payments/orders")
    public Result<StudentPaymentOrderResponse> createPaymentOrder(@Valid @RequestBody StudentPayRequest request) {
        return Result.success(studentPaymentService.createPaymentOrder(request));
    }

    @GetMapping("/payments/orders/{orderNo}")
    public Result<StudentPaymentOrderResponse> getPaymentOrder(
            @PathVariable String orderNo,
            @RequestParam String stuNum
    ) {
        return Result.success(studentPaymentService.getPaymentOrder(orderNo, stuNum));
    }

    @PostMapping("/payments/orders/{orderNo}/simulate-success")
    public Result<StudentPaymentOrderResponse> simulateSuccess(
            @PathVariable String orderNo,
            @RequestParam String stuNum
    ) {
        return Result.success(studentPaymentService.simulatePaymentSuccess(orderNo, stuNum));
    }
}


