package com.example.lowcarbondormitory;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.example.lowcarbondormitory.mapper")
public class LowCarbonDormitoryApplication {

    public static void main(String[] args) {
        SpringApplication.run(LowCarbonDormitoryApplication.class, args);
    }
}