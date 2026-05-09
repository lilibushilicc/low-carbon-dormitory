package com.example.lowcarbondormitory.service.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.lowcarbondormitory.dto.request.AdminCreateRewardRequest;
import com.example.lowcarbondormitory.dto.request.AdminCreateStudentRequest;
import com.example.lowcarbondormitory.dto.request.AdminDeductDormFeeRequest;
import com.example.lowcarbondormitory.dto.request.AdminLoginRequest;
import com.example.lowcarbondormitory.dto.request.AdminUpdateRewardStockRequest;
import com.example.lowcarbondormitory.dto.request.AdminUpdateUtilityRateRequest;
import com.example.lowcarbondormitory.dto.response.AdminCreateStudentResponse;
import com.example.lowcarbondormitory.dto.response.AdminLoginResponse;
import com.example.lowcarbondormitory.entity.AdminAccount;
import com.example.lowcarbondormitory.entity.DormFee;
import com.example.lowcarbondormitory.entity.DormInfo;
import com.example.lowcarbondormitory.entity.RewardItem;
import com.example.lowcarbondormitory.entity.StudentBase;
import com.example.lowcarbondormitory.entity.UtilityRateConfig;
import com.example.lowcarbondormitory.mapper.AdminAccountMapper;
import com.example.lowcarbondormitory.mapper.DormFeeMapper;
import com.example.lowcarbondormitory.mapper.DormInfoMapper;
import com.example.lowcarbondormitory.mapper.RewardItemMapper;
import com.example.lowcarbondormitory.mapper.StudentBaseMapper;
import com.example.lowcarbondormitory.mapper.UtilityRateConfigMapper;
import com.example.lowcarbondormitory.service.auth.TokenService;
import com.example.lowcarbondormitory.service.student.DormFeeAccountService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminManagementService {
    private static final int RATE_SCALE = 4;
    private static final int MONEY_SCALE = 2;
    private static final String SYSTEM_ADMIN = "SYSTEM_ADMIN";

    @Autowired
    private AdminAccountMapper adminAccountMapper;

    @Autowired
    private UtilityRateConfigMapper utilityRateConfigMapper;

    @Autowired
    private StudentBaseMapper studentBaseMapper;

    @Autowired
    private DormInfoMapper dormInfoMapper;

    @Autowired
    private DormFeeMapper dormFeeMapper;

    @Autowired
    private RewardItemMapper rewardItemMapper;

    @Autowired
    private DormFeeAccountService dormFeeAccountService;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public AdminLoginResponse login(AdminLoginRequest request) {
        String username = request.getUsername().trim();
        String password = request.getPassword().trim();
        AdminAccount admin = adminAccountMapper.selectOne(
                new LambdaQueryWrapper<AdminAccount>()
                        .eq(AdminAccount::getUsername, username)
                        .last("LIMIT 1")
        );
        if (admin == null || admin.getStatus() == null || admin.getStatus() != 1) {
            throw new IllegalArgumentException("管理员账号不存在或已停用");
        }
        if (admin.getPassword() == null || !password.equals(admin.getPassword())) {
            throw new IllegalArgumentException("管理员密码错误");
        }

        AdminLoginResponse response = new AdminLoginResponse();
        response.setToken(tokenService.createAdminToken(admin.getAdminId(), admin.getUsername()));
        response.setAdminId(admin.getAdminId());
        response.setUsername(admin.getUsername());
        response.setDisplayName(admin.getDisplayName());
        return response;
    }

    public List<UtilityRateConfig> listUtilityRates() {
        return utilityRateConfigMapper.selectList(
                new LambdaQueryWrapper<UtilityRateConfig>().orderByAsc(UtilityRateConfig::getFeeType)
        );
    }

    @Transactional
    public UtilityRateConfig updateUtilityRate(String feeType, AdminUpdateUtilityRateRequest request) {
        String normalizedFeeType = normalizeFeeType(feeType);
        UtilityRateConfig target = utilityRateConfigMapper.selectById(normalizedFeeType);
        boolean exists = target != null;
        if (!exists) {
            target = new UtilityRateConfig();
            target.setFeeType(normalizedFeeType);
        }
        target.setUnitPrice(scaleRate(request.getUnitPrice()));
        target.setUnitName(request.getUnitName().trim());
        if (exists) {
            utilityRateConfigMapper.updateById(target);
        } else {
            utilityRateConfigMapper.insert(target);
        }
        return utilityRateConfigMapper.selectById(normalizedFeeType);
    }

    @Transactional
    public DormFee deductDormFee(Long dormId, AdminDeductDormFeeRequest request) {
        if (dormId == null) {
            throw new IllegalArgumentException("宿舍ID不能为空");
        }
        DormFee dormFee = dormFeeAccountService.getForUpdate(dormId);
        if (dormFee == null) {
            throw new IllegalArgumentException("未找到宿舍费用账户");
        }

        String feeType = normalizeFeeType(request.getFeeType());
        BigDecimal amount = scaleMoney(request.getAmount());
        BigDecimal balanceAfter = dormFeeAccountService.deductBalance(dormFee, feeType, amount);
        LocalDateTime now = LocalDateTime.now();
        dormFee.setLastDeductTime(now);
        dormFeeMapper.updateById(dormFee);

        jdbcTemplate.update(
                "INSERT INTO student_fee_history "
                        + "(student_id, dorm_id, fee_type, operation_type, pay_type, amount, balance_after, create_time, payer_name, payer_stu_num, payer_account) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                null,
                dormId,
                feeType,
                buildDeductOperationType(request.getRemark()),
                normalizePayType(request.getPayType()),
                amount.negate(),
                balanceAfter,
                now,
                defaultIfBlank(request.getPayerName(), SYSTEM_ADMIN),
                null,
                defaultIfBlank(request.getPayerAccount(), SYSTEM_ADMIN)
        );
        return dormFeeMapper.selectById(dormId);
    }

    @Transactional
    public AdminCreateStudentResponse createStudent(AdminCreateStudentRequest request) {
        String stuNum = request.getStuNum().trim();
        if (studentBaseMapper.selectOne(new LambdaQueryWrapper<StudentBase>().eq(StudentBase::getStuNum, stuNum).last("LIMIT 1")) != null) {
            throw new IllegalArgumentException("学号已存在: " + stuNum);
        }

        DormInfo dormInfo = findOrCreateDorm(
                request.getDormBuilding().trim(),
                request.getDormRoom().trim(),
                resolveBedTotal(request.getBedTotal())
        );
        StudentBase student = buildStudent(request, dormInfo, stuNum);
        studentBaseMapper.insert(student);
        ensureDormFeeExists(dormInfo.getDormId());
        syncDormBedAvailable(dormInfo.getDormId());

        AdminCreateStudentResponse response = new AdminCreateStudentResponse();
        response.setStudentId(student.getStudentId());
        response.setStuNum(student.getStuNum());
        response.setDormId(dormInfo.getDormId());
        return response;
    }

    public List<RewardItem> listRewards() {
        return rewardItemMapper.selectList(
                new LambdaQueryWrapper<RewardItem>()
                        .orderByAsc(RewardItem::getSortOrder)
                        .orderByAsc(RewardItem::getRewardId)
        );
    }

    @Transactional
    public RewardItem createReward(AdminCreateRewardRequest request) {
        RewardItem reward = new RewardItem();
        LocalDateTime now = LocalDateTime.now();
        reward.setRewardName(request.getRewardName().trim());
        reward.setRewardDesc(request.getRewardDesc().trim());
        reward.setPointsCost(request.getPointsCost());
        reward.setImageUrl(trimToNull(request.getImageUrl()));
        reward.setStock(request.getStock());
        reward.setSortOrder(request.getSortOrder() == null ? 999 : request.getSortOrder());
        reward.setStatus(request.getStatus() == null ? 1 : request.getStatus());
        reward.setCreateTime(now);
        reward.setUpdateTime(now);
        rewardItemMapper.insert(reward);
        return reward;
    }

    @Transactional
    public RewardItem updateRewardStock(Long rewardId, AdminUpdateRewardStockRequest request) {
        RewardItem reward = rewardItemMapper.selectById(rewardId);
        if (reward == null) {
            throw new IllegalArgumentException("奖品不存在");
        }
        reward.setStock(request.getStock());
        reward.setUpdateTime(LocalDateTime.now());
        rewardItemMapper.updateById(reward);
        return rewardItemMapper.selectById(rewardId);
    }

    @Transactional
    public void deleteReward(Long rewardId) {
        if (rewardId == null) {
            throw new IllegalArgumentException("奖励ID不能为空");
        }
        RewardItem reward = rewardItemMapper.selectById(rewardId);
        if (reward == null) {
            throw new IllegalArgumentException("奖励不存在");
        }
        rewardItemMapper.deleteById(rewardId);
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
        dormInfo.setCarbonScore(82);
        dormInfoMapper.insert(dormInfo);
        return dormInfo;
    }

    private StudentBase buildStudent(AdminCreateStudentRequest request, DormInfo dormInfo, String stuNum) {
        LocalDateTime now = LocalDateTime.now();
        StudentBase student = new StudentBase();
        student.setStuNum(stuNum);
        student.setName(request.getName().trim());
        student.setPassword(request.getPassword().trim());
        student.setGender(request.getGender() == null ? 1 : request.getGender());
        student.setIdCard(trimToNull(request.getIdCard()));
        student.setPhone(defaultIfBlank(request.getPhone(), "13800000000"));
        student.setCollege(defaultIfBlank(request.getCollege(), "未分配学院"));
        student.setMajor(defaultIfBlank(request.getMajor(), "未分配专业"));
        student.setClassName(defaultIfBlank(request.getClassName(), "未分配班级"));
        student.setGrade(defaultIfBlank(request.getGrade(), "未分配年级"));
        student.setDormBuilding(dormInfo.getDormBuilding());
        student.setDormRoom(dormInfo.getDormRoom());
        student.setDormId(dormInfo.getDormId());
        student.setCarbonScore(request.getCarbonScore() == null ? 0 : Math.max(request.getCarbonScore(), 0));
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

    private void syncDormBedAvailable(Long dormId) {
        DormInfo dormInfo = dormInfoMapper.selectById(dormId);
        if (dormInfo == null) {
            return;
        }
        int bedTotal = dormInfo.getBedTotal() == null ? 4 : Math.max(dormInfo.getBedTotal(), 0);
        Long residentCount = studentBaseMapper.selectCount(new LambdaQueryWrapper<StudentBase>().eq(StudentBase::getDormId, dormId));
        dormInfo.setBedTotal(bedTotal);
        dormInfo.setBedAvailable(Math.max(bedTotal - residentCount.intValue(), 0));
        dormInfoMapper.updateById(dormInfo);
    }

    private int resolveBedTotal(Integer bedTotal) {
        if (bedTotal == null) {
            return 4;
        }
        return Math.max(bedTotal, 1);
    }

    private String buildDormType(int bedTotal) {
        return bedTotal + "人间";
    }

    private String buildDeductOperationType(String remark) {
        String trimmed = trimToNull(remark);
        return trimmed == null ? "DEDUCT" : "DEDUCT:" + trimmed;
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

    private BigDecimal scaleRate(BigDecimal value) {
        return value.setScale(RATE_SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal scaleMoney(BigDecimal value) {
        return value.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    private String normalizePayType(String rawPayType) {
        if (rawPayType == null || rawPayType.trim().isEmpty()) {
            return "SYSTEM";
        }
        String value = rawPayType.trim().toUpperCase(Locale.ROOT);
        if ("SYSTEM".equals(value) || "ALIPAY".equals(value) || "WECHAT".equals(value) || "CASH".equals(value)) {
            return value;
        }
        throw new IllegalArgumentException("支付方式仅支持 SYSTEM/ALIPAY/WECHAT/CASH");
    }

    private String normalizeFeeType(String feeType) {
        if (feeType == null || feeType.trim().isEmpty()) {
            throw new IllegalArgumentException("费用类型不能为空");
        }
        String upper = feeType.trim().toUpperCase(Locale.ROOT);
        if (!"ELECTRIC".equals(upper) && !"WATER".equals(upper)) {
            throw new IllegalArgumentException("费用类型仅支持 ELECTRIC 或 WATER");
        }
        return upper;
    }
}
