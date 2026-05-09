package com.example.lowcarbondormitory.service.student;

import com.example.lowcarbondormitory.dto.request.StudentLoginRequest;
import com.example.lowcarbondormitory.dto.response.StudentLoginResponse;
import com.example.lowcarbondormitory.entity.StudentBase;
import com.example.lowcarbondormitory.service.auth.TokenService;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StudentAuthenticationService {

    private static final Logger log = LoggerFactory.getLogger(StudentAuthenticationService.class);

    @Autowired
    private StudentDormService studentDormService;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Transactional
    public StudentLoginResponse login(StudentLoginRequest request) {
        String loginId = request.getUsername().trim();
        String password = request.getPassword().trim();

        StudentBase student = findStudentByStuNum(loginId);
        if (student == null) {
            log.warn("Student login failed because account does not exist: {}", loginId);
            throw new IllegalArgumentException("\u5b66\u53f7\u4e0d\u5b58\u5728");
        }

        studentDormService.attachResolvedDormId(student);

        String storedPassword = student.getPassword() == null ? null : student.getPassword().trim();
        if (storedPassword == null || !password.equals(storedPassword)) {
            log.warn("Student login failed because password is invalid: {}", loginId);
            throw new IllegalArgumentException("\u5bc6\u7801\u9519\u8bef");
        }

        StudentLoginResponse response = new StudentLoginResponse();
        response.setToken(tokenService.createStudentToken(student.getStuNum()));
        response.setStuNum(student.getStuNum());
        response.setUsername(student.getStuNum());
        response.setStudentInfo(studentDormService.buildStudentProfile(student));

        log.info("Student login succeeded, loginId={}, stuNum={}", loginId, student.getStuNum());
        return response;
    }

    private StudentBase findStudentByStuNum(String loginId) {
        List<StudentBase> matches = jdbcTemplate.query(
                """
                        SELECT student_id, stu_num, name, gender, id_card, phone, college, major,
                               class_name, grade, dorm_building, dorm_room, carbon_score, password,
                               create_time, update_time, dorm_id
                        FROM student_base
                        WHERE stu_num = ?
                        ORDER BY student_id DESC
                        LIMIT 1
                        """,
                (rs, rowNum) -> mapStudentBase(rs),
                loginId
        );
        return matches.isEmpty() ? null : matches.get(0);
    }

    private StudentBase mapStudentBase(ResultSet rs) throws SQLException {
        StudentBase student = new StudentBase();
        student.setStudentId(rs.getLong("student_id"));
        student.setStuNum(rs.getString("stu_num"));
        student.setName(rs.getString("name"));
        student.setGender((Integer) rs.getObject("gender"));
        student.setIdCard(rs.getString("id_card"));
        student.setPhone(rs.getString("phone"));
        student.setCollege(rs.getString("college"));
        student.setMajor(rs.getString("major"));
        student.setClassName(rs.getString("class_name"));
        student.setGrade(rs.getString("grade"));
        student.setDormBuilding(rs.getString("dorm_building"));
        student.setDormRoom(rs.getString("dorm_room"));
        student.setCarbonScore((Integer) rs.getObject("carbon_score"));
        student.setPassword(rs.getString("password"));
        student.setCreateTime(rs.getTimestamp("create_time") == null ? null : rs.getTimestamp("create_time").toLocalDateTime());
        student.setUpdateTime(rs.getTimestamp("update_time") == null ? null : rs.getTimestamp("update_time").toLocalDateTime());
        Object dormId = rs.getObject("dorm_id");
        student.setDormId(dormId == null ? null : ((Number) dormId).longValue());
        return student;
    }
}
