package com.example.lowcarbondormitory.service.student;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.lowcarbondormitory.entity.DormInfo;
import com.example.lowcarbondormitory.entity.StudentBase;
import com.example.lowcarbondormitory.mapper.DormInfoMapper;
import com.example.lowcarbondormitory.mapper.StudentBaseMapper;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class StudentContextService {

    @Autowired
    private StudentBaseMapper studentBaseMapper;

    @Autowired
    private DormInfoMapper dormInfoMapper;

    @Autowired
    private StudentDormService studentDormService;

    public StudentBase getRequiredStudent(String stuNum) {
        if (stuNum == null || stuNum.trim().isEmpty()) {
            throw new IllegalArgumentException("学号不能为空");
        }

        StudentBase student = studentBaseMapper.selectOne(
                new LambdaQueryWrapper<StudentBase>().eq(StudentBase::getStuNum, stuNum.trim())
        );
        if (student == null) {
            throw new IllegalArgumentException("未找到对应学生");
        }
        return studentDormService.attachResolvedDormId(student);
    }

    public StudentBase findStudent(String stuNum) {
        if (stuNum == null || stuNum.trim().isEmpty()) {
            return null;
        }
        StudentBase student = studentBaseMapper.selectOne(
                new LambdaQueryWrapper<StudentBase>().eq(StudentBase::getStuNum, stuNum.trim())
        );
        return studentDormService.attachResolvedDormId(student);
    }

    public StudentBase findStudentById(Long studentId) {
        if (studentId == null) {
            return null;
        }
        StudentBase student = studentBaseMapper.selectById(studentId);
        return studentDormService.attachResolvedDormId(student);
    }

    public Long resolveRequiredDormId(StudentBase student, Long requestedDormId) {
        Long dormId = studentDormService.resolveDormId(student, requestedDormId);
        if (dormId == null) {
            throw new IllegalArgumentException("无法确定学生所属宿舍");
        }
        return dormId;
    }

    public DormInfo getRequiredDormInfo(Long dormId) {
        DormInfo dormInfo = dormInfoMapper.selectById(dormId);
        if (dormInfo == null) {
            throw new IllegalArgumentException("未找到对应宿舍");
        }
        return dormInfo;
    }

    public StudentDormContext getRequiredStudentDormContext(String stuNum, Long requestedDormId) {
        StudentBase student = getRequiredStudent(stuNum);
        Long dormId = resolveRequiredDormId(student, requestedDormId);
        DormInfo dormInfo = getRequiredDormInfo(dormId);
        return new StudentDormContext(student, dormId, dormInfo);
    }

    public List<StudentBase> listDormResidents(Long dormId) {
        DormInfo dormInfo = getRequiredDormInfo(dormId);
        return studentDormService.attachResolvedDormIds(
                studentBaseMapper.selectList(
                        new LambdaQueryWrapper<StudentBase>()
                                .select(
                                        StudentBase::getStudentId,
                                        StudentBase::getStuNum,
                                        StudentBase::getName,
                                        StudentBase::getDormId,
                                        StudentBase::getDormBuilding,
                                        StudentBase::getDormRoom
                                )
                                .and(wrapper -> wrapper
                                        .eq(StudentBase::getDormId, dormId)
                                        .or(inner -> inner
                                                .eq(StudentBase::getDormBuilding, dormInfo.getDormBuilding())
                                                .eq(StudentBase::getDormRoom, dormInfo.getDormRoom())
                                        )
                                )
                                .orderByAsc(StudentBase::getStudentId)
                )
        );
    }

    public record StudentDormContext(
            StudentBase student,
            Long dormId,
            DormInfo dormInfo
    ) {
    }
}
