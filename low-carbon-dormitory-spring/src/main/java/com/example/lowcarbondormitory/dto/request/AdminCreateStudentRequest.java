package com.example.lowcarbondormitory.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AdminCreateStudentRequest {

    @NotBlank(message = "学号不能为空")
    private String stuNum;

    @NotBlank(message = "姓名不能为空")
    private String name;

    @NotBlank(message = "密码不能为空")
    private String password;

    @NotBlank(message = "宿舍楼不能为空")
    private String dormBuilding;

    @NotBlank(message = "宿舍号不能为空")
    private String dormRoom;

    @Min(value = 1, message = "床位总数必须大于等于1")
    private Integer bedTotal;

    private Integer gender;
    private String idCard;
    private String phone;
    private String college;
    private String major;
    private String className;
    private String grade;

    @Min(value = 0, message = "碳积分不能小于0")
    private Integer carbonScore;
}
