package com.example.lowcarbondormitory.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AdminStudentListItemResponse {

    private Long studentId;
    private String stuNum;
    private String name;
    private Integer gender;
    private String phone;
    private String college;
    private String major;
    private String className;
    private String grade;
    private Integer carbonScore;
    private Long dormId;
    private String dormBuilding;
    private String dormRoom;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
