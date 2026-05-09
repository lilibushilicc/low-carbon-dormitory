package com.example.lowcarbondormitory.dto.response;

import lombok.Data;

import java.time.LocalDate;

@Data
public class StudentProfileResponse {
    private Long studentId;
    private Long dormId;
    private String stuNum;
    private String dormNo;
    private String name;
    private Integer gender;
    private String idCard;
    private String phone;
    private String college;
    private String major;
    private String className;
    private String grade;
    private String username;
    private String signature;
    private LocalDate admissionDate;
    private String dormBuilding;
    private String dormRoom;
    private String dormType;
    private Integer bedTotal;
    private Integer bedAvailable;
    private Integer carbonScore;
}

