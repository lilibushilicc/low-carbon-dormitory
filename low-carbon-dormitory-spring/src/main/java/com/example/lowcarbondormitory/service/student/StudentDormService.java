package com.example.lowcarbondormitory.service.student;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.lowcarbondormitory.dto.response.StudentProfileResponse;
import com.example.lowcarbondormitory.dto.response.StudentWaterElectricityResponse;
import com.example.lowcarbondormitory.entity.DormFee;
import com.example.lowcarbondormitory.entity.DormInfo;
import com.example.lowcarbondormitory.entity.StudentBase;
import com.example.lowcarbondormitory.entity.StudentExt;
import com.example.lowcarbondormitory.entity.UtilityRateConfig;
import com.example.lowcarbondormitory.mapper.DormInfoMapper;
import com.example.lowcarbondormitory.mapper.StudentExtMapper;
import com.example.lowcarbondormitory.mapper.UtilityRateConfigMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class StudentDormService {

    @Autowired
    private DormInfoMapper dormInfoMapper;

    @Autowired
    private StudentExtMapper studentExtMapper;

    @Autowired
    private UtilityRateConfigMapper utilityRateConfigMapper;

    @Autowired
    private DormFeeAccountService dormFeeAccountService;

    public Long resolveDormId(StudentBase student, Long requestedDormId) {
        if (requestedDormId != null) {
            return requestedDormId;
        }
        DormInfo dormInfo = resolveDormInfo(student);
        if (student != null && student.getDormId() != null) {
            return student.getDormId();
        }
        return dormInfo == null ? null : dormInfo.getDormId();
    }

    public StudentBase attachResolvedDormId(StudentBase student) {
        DormInfo dormInfo = resolveDormInfo(student);
        if (dormInfo != null) {
            student.setDormId(dormInfo.getDormId());
        }
        return student;
    }

    public List<StudentBase> attachResolvedDormIds(List<StudentBase> students) {
        if (students == null || students.isEmpty()) {
            return students;
        }

        Set<Long> dormIds = students.stream()
                .map(StudentBase::getDormId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, DormInfo> dormInfoById = loadDormInfoByIds(dormIds);
        Map<String, DormInfo> dormInfoByCompositeKey = new HashMap<>();

        students.forEach(student -> attachResolvedDormId(student, dormInfoById, dormInfoByCompositeKey));
        return students;
    }

    public DormInfo getDormInfo(Long dormId) {
        if (dormId == null) {
            return null;
        }
        return dormInfoMapper.selectById(dormId);
    }

    public DormInfo findDormInfoByStudent(StudentBase student) {
        return resolveDormInfo(student);
    }

    private DormInfo resolveDormInfo(StudentBase student) {
        if (student == null) {
            return null;
        }
        if (student.getDormId() != null) {
            return getDormInfo(student.getDormId());
        }
        return findDormInfoByLocation(student.getDormBuilding(), student.getDormRoom());
    }

    private StudentBase attachResolvedDormId(
            StudentBase student,
            Map<Long, DormInfo> dormInfoById,
            Map<String, DormInfo> dormInfoByCompositeKey
    ) {
        if (student == null || student.getDormId() != null) {
            return student;
        }

        DormInfo dormInfo = resolveDormInfo(student, dormInfoById, dormInfoByCompositeKey);
        if (dormInfo != null) {
            student.setDormId(dormInfo.getDormId());
        }
        return student;
    }

    private DormInfo resolveDormInfo(
            StudentBase student,
            Map<Long, DormInfo> dormInfoById,
            Map<String, DormInfo> dormInfoByCompositeKey
    ) {
        if (student == null) {
            return null;
        }
        if (student.getDormId() != null) {
            return dormInfoById.get(student.getDormId());
        }
        return getDormInfoByCompositeKey(dormInfoByCompositeKey, student.getDormBuilding(), student.getDormRoom());
    }

    private DormInfo findDormInfoByLocation(String dormBuilding, String dormRoom) {
        if (isBlank(dormBuilding) || isBlank(dormRoom)) {
            return null;
        }
        return dormInfoMapper.selectOne(
                new LambdaQueryWrapper<DormInfo>()
                        .eq(DormInfo::getDormBuilding, dormBuilding.trim())
                        .eq(DormInfo::getDormRoom, dormRoom.trim())
                        .last("LIMIT 1")
        );
    }

    private DormInfo getDormInfoByCompositeKey(Map<String, DormInfo> dormInfoByCompositeKey, String dormBuilding, String dormRoom) {
        if (isBlank(dormBuilding) || isBlank(dormRoom)) {
            return null;
        }
        String dormKey = buildDormKey(dormBuilding, dormRoom);
        if (dormInfoByCompositeKey.containsKey(dormKey)) {
            return dormInfoByCompositeKey.get(dormKey);
        }
        DormInfo dormInfo = findDormInfoByLocation(dormBuilding, dormRoom);
        if (dormInfo != null) {
            dormInfoByCompositeKey.put(dormKey, dormInfo);
        }
        return dormInfo;
    }

    private Map<Long, DormInfo> loadDormInfoByIds(Set<Long> dormIds) {
        if (dormIds == null || dormIds.isEmpty()) {
            return Map.of();
        }
        return dormInfoMapper.selectBatchIds(dormIds).stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(
                        DormInfo::getDormId,
                        Function.identity(),
                        (left, right) -> left
                ));
    }

    private String buildDormKey(String dormBuilding, String dormRoom) {
        return safeTrim(dormBuilding) + "#" + safeTrim(dormRoom);
    }

    public DormFee getDormFee(Long dormId) {
        return dormFeeAccountService.getOrCreate(dormId);
    }

    public StudentProfileResponse buildStudentProfile(StudentBase student) {
        StudentExt ext = studentExtMapper.selectOne(
                new LambdaQueryWrapper<StudentExt>().eq(StudentExt::getStudentId, student.getStudentId())
        );
        DormInfo dormInfo = findDormInfoByStudent(student);
        Long resolvedDormId = resolveDormId(student, null);

        StudentProfileResponse response = new StudentProfileResponse();
        response.setStudentId(student.getStudentId());
        response.setStuNum(student.getStuNum());
        response.setName(student.getName());
        response.setGender(student.getGender());
        response.setIdCard(student.getIdCard());
        response.setPhone(student.getPhone());
        response.setCollege(student.getCollege());
        response.setMajor(student.getMajor());
        response.setClassName(student.getClassName());
        response.setGrade(student.getGrade());
        response.setDormId(resolvedDormId);
        response.setUsername(student.getStuNum());
        response.setCarbonScore(student.getCarbonScore());
        applyDormLocation(response, student.getDormBuilding(), student.getDormRoom());

        if (ext != null) {
            response.setSignature(ext.getSignature());
            response.setAdmissionDate(ext.getAdmissionDate());
        }
        if (dormInfo != null) {
            response.setDormType(dormInfo.getDormType());
            response.setBedTotal(dormInfo.getBedTotal());
            response.setBedAvailable(dormInfo.getBedAvailable());
            response.setDormNo(buildDormNo(dormInfo));
        }

        return response;
    }

    public StudentWaterElectricityResponse buildWaterElectricityResponse(
            StudentBase student,
            Long dormId,
            DormFee dormFee,
            DormInfo dormInfo
    ) {
        UtilityRateConfig electricRate = getRequiredRate("ELECTRIC");
        UtilityRateConfig waterRate = getRequiredRate("WATER");

        StudentWaterElectricityResponse response = new StudentWaterElectricityResponse();
        response.setStuNum(student == null ? null : student.getStuNum());
        response.setDormId(dormId);
        response.setWaterBalance(dormFee == null ? null : dormFee.getWaterBalance());
        response.setElectricityBalance(dormFee == null ? null : dormFee.getElectricityBalance());
        response.setLastDeductTime(dormFee == null ? null : dormFee.getLastDeductTime());
        response.setElectricityUnitPrice(electricRate.getUnitPrice());
        response.setWaterUnitPrice(waterRate.getUnitPrice());
        response.setElectricityUnitName(electricRate.getUnitName());
        response.setWaterUnitName(waterRate.getUnitName());
        response.setElectricityBillingEnabled(electricRate.isBillingEnabled());
        response.setWaterBillingEnabled(waterRate.isBillingEnabled());
        response.setElectricityAvailable(calculateAvailableQuantity(response.getElectricityBalance(), electricRate.getUnitPrice()));
        response.setWaterAvailable(waterRate.isBillingEnabled()
                ? calculateAvailableQuantity(response.getWaterBalance(), waterRate.getUnitPrice())
                : BigDecimal.ZERO);

        if (student != null) {
            applyDormLocation(response, student.getDormBuilding(), student.getDormRoom());
            response.setPersonalCarbonScore(student.getCarbonScore());
        }
        if (dormInfo != null) {
            applyDormLocation(response, dormInfo.getDormBuilding(), dormInfo.getDormRoom());
            response.setDormCarbonScore(dormInfo.getCarbonScore());
        }
        return response;
    }

    public String buildDormNo(DormInfo dormInfo) {
        if (dormInfo == null) {
            return null;
        }
        return buildDormNo(dormInfo.getDormBuilding(), dormInfo.getDormRoom());
    }

    public String buildDormNo(String dormBuilding, String dormRoom) {
        String building = safeTrim(dormBuilding);
        String room = safeTrim(dormRoom);
        if (building.isEmpty()) {
            return room.isEmpty() ? null : room;
        }
        return room.isEmpty() ? building : building + "-" + room;
    }

    private void applyDormLocation(StudentProfileResponse response, String dormBuilding, String dormRoom) {
        response.setDormBuilding(dormBuilding);
        response.setDormRoom(dormRoom);
        response.setDormNo(buildDormNo(dormBuilding, dormRoom));
    }

    private void applyDormLocation(StudentWaterElectricityResponse response, String dormBuilding, String dormRoom) {
        response.setDormBuilding(dormBuilding);
        response.setDormRoom(dormRoom);
        response.setDormNo(buildDormNo(dormBuilding, dormRoom));
    }

    private UtilityRateConfig getRequiredRate(String feeType) {
        UtilityRateConfig rateConfig = utilityRateConfigMapper.selectById(feeType);
        if (rateConfig == null || rateConfig.getUnitPrice() == null || rateConfig.getUnitPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("濮樺鏁搁幑銏㈢暬闁板秶鐤嗙紓鍝勩亼: " + feeType);
        }
        return rateConfig;
    }

    private BigDecimal calculateAvailableQuantity(BigDecimal balance, BigDecimal unitPrice) {
        BigDecimal safeBalance = balance == null ? BigDecimal.ZERO : balance;
        if (unitPrice == null || unitPrice.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return safeBalance.divide(unitPrice, 2, RoundingMode.DOWN);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }
}

