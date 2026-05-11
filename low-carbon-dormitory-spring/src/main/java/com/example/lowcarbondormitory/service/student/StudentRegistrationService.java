package com.example.lowcarbondormitory.service.student;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.lowcarbondormitory.dto.request.AdminCreateStudentRequest;
import com.example.lowcarbondormitory.dto.request.StudentRegisterRequest;
import com.example.lowcarbondormitory.dto.response.AdminCreateStudentResponse;
import com.example.lowcarbondormitory.dto.response.StudentRegisterResponse;
import com.example.lowcarbondormitory.entity.DormFee;
import com.example.lowcarbondormitory.entity.DormInfo;
import com.example.lowcarbondormitory.entity.StudentBase;
import com.example.lowcarbondormitory.mapper.DormFeeMapper;
import com.example.lowcarbondormitory.mapper.DormInfoMapper;
import com.example.lowcarbondormitory.mapper.StudentBaseMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StudentRegistrationService {

    private static final int DEFAULT_BED_TOTAL = 4;
    private static final int MONEY_SCALE = 2;
    private static final int DEFAULT_GENDER = 1;
    private static final int DEFAULT_CARBON_SCORE = 0;
    private static final int DEFAULT_DORM_CARBON_SCORE = 82;
    private static final String DEFAULT_PHONE = "13800000000";
    private static final String DEFAULT_COLLEGE = "未分配学院";
    private static final String DEFAULT_MAJOR = "未分配专业";
    private static final String DEFAULT_CLASS_NAME = "未分配班级";
    private static final String DEFAULT_GRADE = "未分配年级";

    @Autowired
    private StudentBaseMapper studentBaseMapper;

    @Autowired
    private DormInfoMapper dormInfoMapper;

    @Autowired
    private DormFeeMapper dormFeeMapper;

    @Transactional
    public AdminCreateStudentResponse createByAdmin(AdminCreateStudentRequest request) {
        StudentBase student = register(
                request.getStuNum(),
                request.getName(),
                request.getPassword(),
                request.getDormBuilding(),
                request.getDormRoom(),
                request.getBedTotal(),
                request.getGender(),
                request.getIdCard(),
                request.getPhone(),
                request.getCollege(),
                request.getMajor(),
                request.getClassName(),
                request.getGrade(),
                request.getCarbonScore()
        );
        return toAdminCreateStudentResponse(student);
    }

    @Transactional
    public StudentRegisterResponse register(StudentRegisterRequest request) {
        StudentBase student = register(
                request.getStuNum(),
                request.getName(),
                request.getPassword(),
                request.getDormBuilding(),
                request.getDormRoom(),
                request.getBedTotal(),
                request.getGender(),
                request.getIdCard(),
                request.getPhone(),
                request.getCollege(),
                request.getMajor(),
                request.getClassName(),
                request.getGrade(),
                DEFAULT_CARBON_SCORE
        );
        return toStudentRegisterResponse(student);
    }

    private StudentBase register(
            String stuNumRaw,
            String nameRaw,
            String passwordRaw,
            String dormBuildingRaw,
            String dormRoomRaw,
            Integer bedTotal,
            Integer gender,
            String idCard,
            String phone,
            String college,
            String major,
            String className,
            String grade,
            Integer carbonScore
    ) {
        String stuNum = requireTrimmed(stuNumRaw, "学号不能为空");
        if (findStudentByStuNum(stuNum) != null) {
            throw new IllegalArgumentException("学号已存在: " + stuNum);
        }

        DormInfo dormInfo = findOrCreateDorm(
                requireTrimmed(dormBuildingRaw, "宿舍楼不能为空"),
                requireTrimmed(dormRoomRaw, "宿舍房间不能为空"),
                resolveBedTotal(bedTotal)
        );

        StudentBase student = buildStudent(
                stuNum,
                requireTrimmed(nameRaw, "姓名不能为空"),
                requireTrimmed(passwordRaw, "密码不能为空"),
                gender,
                idCard,
                phone,
                college,
                major,
                className,
                grade,
                carbonScore,
                dormInfo
        );
        studentBaseMapper.insert(student);
        ensureDormFeeExists(dormInfo.getDormId());
        syncDormBedAvailability(dormInfo.getDormId());
        return student;
    }

    private StudentBase findStudentByStuNum(String stuNum) {
        return studentBaseMapper.selectOne(
                new LambdaQueryWrapper<StudentBase>()
                        .eq(StudentBase::getStuNum, stuNum)
                        .last("LIMIT 1")
        );
    }

    private DormInfo findOrCreateDorm(String dormBuilding, String dormRoom, int requestedBedTotal) {
        DormInfo dormInfo = dormInfoMapper.selectOne(
                new LambdaQueryWrapper<DormInfo>()
                        .eq(DormInfo::getDormBuilding, dormBuilding)
                        .eq(DormInfo::getDormRoom, dormRoom)
                        .last("LIMIT 1")
        );
        if (dormInfo != null) {
            Integer existingBedTotal = dormInfo.getBedTotal();
            if (existingBedTotal == null || existingBedTotal <= 0) {
                dormInfo.setBedTotal(requestedBedTotal);
                dormInfo.setDormType(buildDormType(requestedBedTotal));
                dormInfoMapper.updateById(dormInfo);
            } else if (existingBedTotal != requestedBedTotal) {
                throw new IllegalArgumentException("宿舍已存在，且床位数为 " + existingBedTotal + "，与本次录入不一致");
            }
            return dormInfo;
        }

        dormInfo = new DormInfo();
        dormInfo.setDormBuilding(dormBuilding);
        dormInfo.setDormRoom(dormRoom);
        dormInfo.setBedTotal(requestedBedTotal);
        dormInfo.setBedAvailable(requestedBedTotal);
        dormInfo.setDormType(buildDormType(requestedBedTotal));
        dormInfo.setCarbonScore(DEFAULT_DORM_CARBON_SCORE);
        dormInfoMapper.insert(dormInfo);
        return dormInfo;
    }

    private StudentBase buildStudent(
            String stuNum,
            String name,
            String password,
            Integer gender,
            String idCard,
            String phone,
            String college,
            String major,
            String className,
            String grade,
            Integer carbonScore,
            DormInfo dormInfo
    ) {
        LocalDateTime now = LocalDateTime.now();
        StudentBase student = new StudentBase();
        student.setStuNum(stuNum);
        student.setName(name);
        student.setPassword(password);
        student.setGender(gender == null ? DEFAULT_GENDER : gender);
        student.setIdCard(trimToNull(idCard));
        student.setPhone(defaultIfBlank(phone, DEFAULT_PHONE));
        student.setCollege(defaultIfBlank(college, DEFAULT_COLLEGE));
        student.setMajor(defaultIfBlank(major, DEFAULT_MAJOR));
        student.setClassName(defaultIfBlank(className, DEFAULT_CLASS_NAME));
        student.setGrade(defaultIfBlank(grade, DEFAULT_GRADE));
        student.setDormBuilding(dormInfo.getDormBuilding());
        student.setDormRoom(dormInfo.getDormRoom());
        student.setDormId(dormInfo.getDormId());
        student.setCarbonScore(carbonScore == null ? DEFAULT_CARBON_SCORE : Math.max(carbonScore, 0));
        student.setCreateTime(now);
        student.setUpdateTime(now);
        return student;
    }

    private void ensureDormFeeExists(Long dormId) {
        if (dormId == null || dormFeeMapper.selectById(dormId) != null) {
            return;
        }
        DormFee dormFee = new DormFee();
        dormFee.setDormId(dormId);
        dormFee.setElectricityBalance(scaleMoney(BigDecimal.ZERO));
        dormFee.setWaterBalance(scaleMoney(BigDecimal.ZERO));
        dormFeeMapper.insert(dormFee);
    }

    @Transactional
    public void syncDormBedAvailability(Long dormId) {
        DormInfo dormInfo = dormInfoMapper.selectById(dormId);
        if (dormInfo == null) {
            return;
        }
        int bedTotal = dormInfo.getBedTotal() == null ? DEFAULT_BED_TOTAL : Math.max(dormInfo.getBedTotal(), 0);
        Long residentCount = studentBaseMapper.selectCount(
                new LambdaQueryWrapper<StudentBase>().eq(StudentBase::getDormId, dormId)
        );
        dormInfo.setBedTotal(bedTotal);
        dormInfo.setBedAvailable(Math.max(bedTotal - residentCount.intValue(), 0));
        dormInfoMapper.updateById(dormInfo);
    }

    private int resolveBedTotal(Integer bedTotal) {
        if (bedTotal == null) {
            return DEFAULT_BED_TOTAL;
        }
        return Math.max(bedTotal, 1);
    }

    private String buildDormType(int bedTotal) {
        return bedTotal + "人间";
    }

    private String requireTrimmed(String value, String message) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            throw new IllegalArgumentException(message);
        }
        return trimmed;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String defaultIfBlank(String value, String defaultValue) {
        String trimmed = trimToNull(value);
        return trimmed == null ? defaultValue : trimmed;
    }

    private AdminCreateStudentResponse toAdminCreateStudentResponse(StudentBase student) {
        AdminCreateStudentResponse response = new AdminCreateStudentResponse();
        response.setStudentId(student.getStudentId());
        response.setStuNum(student.getStuNum());
        response.setDormId(student.getDormId());
        return response;
    }

    private StudentRegisterResponse toStudentRegisterResponse(StudentBase student) {
        StudentRegisterResponse response = new StudentRegisterResponse();
        response.setStudentId(student.getStudentId());
        response.setStuNum(student.getStuNum());
        response.setDormId(student.getDormId());
        return response;
    }

    private BigDecimal scaleMoney(BigDecimal value) {
        return value.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }
}
