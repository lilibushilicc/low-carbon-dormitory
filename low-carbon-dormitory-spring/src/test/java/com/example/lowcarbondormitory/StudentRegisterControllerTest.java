package com.example.lowcarbondormitory;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class StudentRegisterControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldAllowPublicStudentRegisterAndLogin() throws Exception {
        String stuNum = "REG" + System.currentTimeMillis();
        String password = "register123";

        mockMvc.perform(post("/student/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "stuNum": "%s",
                                  "name": "开放注册测试",
                                  "password": "%s",
                                  "dormBuilding": "测试楼",
                                  "dormRoom": "520",
                                  "bedTotal": 4,
                                  "gender": 1,
                                  "phone": "13812345678",
                                  "college": "测试学院",
                                  "major": "测试专业",
                                  "className": "测试班",
                                  "grade": "2026级"
                                }
                                """.formatted(stuNum, password)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.stuNum").value(stuNum))
                .andExpect(jsonPath("$.data.studentId").isNumber())
                .andExpect(jsonPath("$.data.dormId").isNumber());

        mockMvc.perform(post("/student/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "%s",
                                  "password": "%s"
                                }
                                """.formatted(stuNum, password)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.stuNum").value(stuNum))
                .andExpect(jsonPath("$.data.token").isNotEmpty());
    }
}
