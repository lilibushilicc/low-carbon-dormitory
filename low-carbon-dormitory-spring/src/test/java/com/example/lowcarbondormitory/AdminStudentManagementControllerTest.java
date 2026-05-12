package com.example.lowcarbondormitory;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.lowcarbondormitory.entity.StudentBase;
import com.example.lowcarbondormitory.entity.StudentExt;
import com.example.lowcarbondormitory.mapper.StudentBaseMapper;
import com.example.lowcarbondormitory.mapper.StudentExtMapper;
import com.example.lowcarbondormitory.service.auth.TokenService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AdminStudentManagementControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private StudentBaseMapper studentBaseMapper;

    @Autowired
    private StudentExtMapper studentExtMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldDeleteStudentWithoutBusinessHistoryAndSyncDormData() throws Exception {
        String stuNum = "ADMDEL" + System.currentTimeMillis();
        String token = adminToken();

        mockMvc.perform(post("/admin/students")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "stuNum": "%s",
                                  "name": "管理员删除测试学生",
                                  "password": "delete123",
                                  "dormBuilding": "删除测试楼",
                                  "dormRoom": "801",
                                  "bedTotal": 4,
                                  "gender": 1,
                                  "college": "测试学院",
                                  "major": "测试专业",
                                  "className": "测试班级",
                                  "grade": "2026级"
                                }
                                """.formatted(stuNum)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        StudentBase created = findStudentByStuNum(stuNum);
        assertThat(created).isNotNull();
        assertThat(created.getDormId()).isNotNull();

        StudentExt ext = new StudentExt();
        ext.setStudentId(created.getStudentId());
        ext.setAvatar("https://example.com/avatar.png");
        ext.setSignature("待删除扩展档案");
        studentExtMapper.insert(ext);

        mockMvc.perform(get("/admin/students")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[?(@.stuNum=='" + stuNum + "')]").isNotEmpty());

        mockMvc.perform(get("/admin/students/{studentId}/delete-check", created.getStudentId())
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.deletable").value(true));

        mockMvc.perform(delete("/admin/students/{studentId}", created.getStudentId())
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(true));

        assertThat(findStudentByStuNum(stuNum)).isNull();
        assertThat(studentExtMapper.selectCount(
                new LambdaQueryWrapper<StudentExt>().eq(StudentExt::getStudentId, created.getStudentId())
        )).isZero();

        Integer bedAvailable = jdbcTemplate.queryForObject(
                "SELECT bed_available FROM student_dorm_info WHERE dorm_id = ?",
                Integer.class,
                created.getDormId()
        );
        assertThat(bedAvailable).isEqualTo(4);
    }

    @Test
    void shouldRejectDeleteWhenStudentHasPaymentHistory() throws Exception {
        String stuNum = "ADMKEEP" + System.currentTimeMillis();
        String token = adminToken();

        mockMvc.perform(post("/admin/students")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "stuNum": "%s",
                                  "name": "管理员保留测试学生",
                                  "password": "keep123",
                                  "dormBuilding": "删除约束楼",
                                  "dormRoom": "901",
                                  "bedTotal": 4,
                                  "gender": 1
                                }
                                """.formatted(stuNum)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        StudentBase created = findStudentByStuNum(stuNum);
        assertThat(created).isNotNull();

        jdbcTemplate.update(
                "INSERT INTO student_payment_order "
                        + "(order_no, student_id, stu_num, dorm_id, fee_type, pay_type, amount, payer_account, status, qr_code_content, create_time) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                "TEST" + System.currentTimeMillis(),
                created.getStudentId(),
                created.getStuNum(),
                created.getDormId(),
                "ELECTRIC",
                "ALIPAY",
                10.00,
                created.getStuNum(),
                "PENDING",
                "lowcarbon://pay-test",
                LocalDateTime.now()
        );

        mockMvc.perform(get("/admin/students/{studentId}/delete-check", created.getStudentId())
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.deletable").value(false))
                .andExpect(jsonPath("$.data.paymentOrderCount").value(1));

        mockMvc.perform(delete("/admin/students/{studentId}", created.getStudentId())
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.msg").value(org.hamcrest.Matchers.containsString("支付订单记录")));

        assertThat(findStudentByStuNum(stuNum)).isNotNull();
    }

    private StudentBase findStudentByStuNum(String stuNum) {
        return studentBaseMapper.selectOne(
                new LambdaQueryWrapper<StudentBase>()
                        .eq(StudentBase::getStuNum, stuNum)
                        .last("LIMIT 1")
        );
    }

    private String adminToken() {
        return tokenService.createAdminToken(1L, "admin");
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
