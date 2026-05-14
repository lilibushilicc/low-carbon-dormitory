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
import com.example.lowcarbondormitory.dto.response.AdminStudentDeleteCheckResponse;
import com.example.lowcarbondormitory.dto.response.AdminStudentListItemResponse;
import com.example.lowcarbondormitory.entity.AdminAccount;
import com.example.lowcarbondormitory.entity.DormFee;
import com.example.lowcarbondormitory.entity.RewardItem;
import com.example.lowcarbondormitory.entity.StudentBase;
import com.example.lowcarbondormitory.entity.StudentExt;
import com.example.lowcarbondormitory.entity.UtilityRateConfig;
import com.example.lowcarbondormitory.mapper.AdminAccountMapper;
import com.example.lowcarbondormitory.mapper.DormFeeMapper;
import com.example.lowcarbondormitory.mapper.RewardItemMapper;
import com.example.lowcarbondormitory.mapper.StudentBaseMapper;
import com.example.lowcarbondormitory.mapper.StudentExtMapper;
import com.example.lowcarbondormitory.mapper.UtilityRateConfigMapper;
import com.example.lowcarbondormitory.service.auth.TokenService;
import com.example.lowcarbondormitory.service.student.DormFeeAccountService;
import com.example.lowcarbondormitory.service.student.StudentRegistrationService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
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
    private DormFeeMapper dormFeeMapper;

    @Autowired
    private RewardItemMapper rewardItemMapper;

    @Autowired
    private DormFeeAccountService dormFeeAccountService;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private StudentRegistrationService studentRegistrationService;

    @Autowired
    private StudentBaseMapper studentBaseMapper;

    @Autowired
    private StudentExtMapper studentExtMapper;

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
        target.setEnabled(request.getEnabled() == null ? Boolean.TRUE : request.getEnabled());
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
        assertBillingEnabled(feeType);
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
        return studentRegistrationService.createByAdmin(request);
    }

    public List<AdminStudentListItemResponse> listStudents() {
        return studentBaseMapper.selectList(
                        new LambdaQueryWrapper<StudentBase>()
                                .orderByDesc(StudentBase::getCreateTime)
                                .orderByDesc(StudentBase::getStudentId)
                ).stream()
                .map(this::toAdminStudentListItem)
                .toList();
    }

    public AdminStudentDeleteCheckResponse getDeleteCheck(Long studentId) {
        return buildDeleteCheck(requireStudent(studentId));
    }

    @Transactional
    public void deleteStudent(Long studentId) {
        StudentBase student = requireStudent(studentId);
        AdminStudentDeleteCheckResponse deleteCheck = buildDeleteCheck(student);
        if (!deleteCheck.isDeletable()) {
            throw new IllegalStateException(deleteCheck.getReason());
        }

        studentExtMapper.delete(new LambdaQueryWrapper<StudentExt>().eq(StudentExt::getStudentId, studentId));
        studentBaseMapper.deleteById(studentId);
        studentRegistrationService.syncDormBedAvailability(student.getDormId());
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

    private StudentBase requireStudent(Long studentId) {
        if (studentId == null) {
            throw new IllegalArgumentException("学生ID不能为空");
        }
        StudentBase student = studentBaseMapper.selectById(studentId);
        if (student == null) {
            throw new IllegalArgumentException("学生不存在");
        }
        return student;
    }

    private AdminStudentDeleteCheckResponse buildDeleteCheck(StudentBase student) {
        int rewardExchangeCount = countInt(
                "SELECT COUNT(1) FROM student_reward_exchange WHERE student_id = ?",
                student.getStudentId()
        );
        int feeHistoryCount = countInt(
                "SELECT COUNT(1) FROM student_fee_history WHERE student_id = ?",
                student.getStudentId()
        );
        int paymentOrderCount = countInt(
                "SELECT COUNT(1) FROM student_payment_order WHERE student_id = ?",
                student.getStudentId()
        );

        List<String> reasons = new ArrayList<>();
        appendDeleteReason(reasons, rewardExchangeCount, "奖励兑换记录");
        appendDeleteReason(reasons, feeHistoryCount, "费用流水记录");
        appendDeleteReason(reasons, paymentOrderCount, "支付订单记录");

        AdminStudentDeleteCheckResponse response = new AdminStudentDeleteCheckResponse();
        response.setStudentId(student.getStudentId());
        response.setStuNum(student.getStuNum());
        response.setRewardExchangeCount(rewardExchangeCount);
        response.setFeeHistoryCount(feeHistoryCount);
        response.setPaymentOrderCount(paymentOrderCount);
        response.setDeletable(reasons.isEmpty());
        response.setReason(reasons.isEmpty() ? "可删除" : "该学生不能删除：" + String.join("、", reasons));
        return response;
    }

    private int countInt(String sql, Object... args) {
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, args);
        return count == null ? 0 : Math.max(count, 0);
    }

    private void appendDeleteReason(List<String> reasons, int count, String label) {
        if (count > 0) {
            reasons.add("已有" + count + "条" + label);
        }
    }

    private AdminStudentListItemResponse toAdminStudentListItem(StudentBase student) {
        AdminStudentListItemResponse response = new AdminStudentListItemResponse();
        response.setStudentId(student.getStudentId());
        response.setStuNum(student.getStuNum());
        response.setName(student.getName());
        response.setGender(student.getGender());
        response.setPhone(student.getPhone());
        response.setCollege(student.getCollege());
        response.setMajor(student.getMajor());
        response.setClassName(student.getClassName());
        response.setGrade(student.getGrade());
        response.setCarbonScore(student.getCarbonScore());
        response.setDormId(student.getDormId());
        response.setDormBuilding(student.getDormBuilding());
        response.setDormRoom(student.getDormRoom());
        response.setCreateTime(student.getCreateTime());
        response.setUpdateTime(student.getUpdateTime());
        return response;
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

    private void assertBillingEnabled(String feeType) {
        UtilityRateConfig rateConfig = utilityRateConfigMapper.selectById(feeType);
        if (rateConfig == null) {
            throw new IllegalStateException("缂哄皯璐圭巼閰嶇疆: " + feeType);
        }
        if (!rateConfig.isBillingEnabled()) {
            throw new IllegalStateException(feeType + " 宸插仠鐢ㄨ璐癸紝鏃犳硶鎵ц鎵ｈ垂");
        }
    }
}
