package com.example.lowcarbondormitory.service.student;

import com.example.lowcarbondormitory.dto.request.StudentPayRequest;
import com.example.lowcarbondormitory.dto.response.StudentPaymentOrderResponse;
import com.example.lowcarbondormitory.dto.response.StudentWaterElectricityResponse;
import com.example.lowcarbondormitory.entity.DormFee;
import com.example.lowcarbondormitory.entity.DormFeeHistory;
import com.example.lowcarbondormitory.entity.DormInfo;
import com.example.lowcarbondormitory.entity.StudentBase;
import com.example.lowcarbondormitory.entity.UtilityRateConfig;
import com.example.lowcarbondormitory.mapper.DormFeeMapper;
import com.example.lowcarbondormitory.mapper.DormInfoMapper;
import com.example.lowcarbondormitory.mapper.UtilityRateConfigMapper;
import com.example.lowcarbondormitory.service.dashboard.LowCarbonDashboardSupport;
import com.example.lowcarbondormitory.service.rule.LowCarbonRuleService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StudentPaymentService {

    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_SUCCESS = "SUCCESS";
    private static final String FEE_TYPE_WATER = "WATER";
    private static final String FEE_TYPE_ELECTRIC = "ELECTRIC";
    private static final String OPERATION_RECHARGE = "RECHARGE";
    private static final String OPERATION_REFRESH = "REFRESH";
    private static final String PAY_TYPE_SYSTEM = "SYSTEM";
    private static final int MAX_HISTORY_RECORDS_PER_DORM = 30;

    @Autowired
    private StudentContextService studentContextService;

    @Autowired
    private DormFeeMapper dormFeeMapper;

    @Autowired
    private DormInfoMapper dormInfoMapper;

    @Autowired
    private StudentDormService studentDormService;

    @Autowired
    private DormFeeAccountService dormFeeAccountService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private LowCarbonRuleService lowCarbonRuleService;

    @Autowired
    private UtilityRateConfigMapper utilityRateConfigMapper;

    @Transactional(rollbackFor = Exception.class)
    public StudentWaterElectricityResponse pay(StudentPayRequest request) {
        return applyRecharge(request);
    }

    @Transactional(rollbackFor = Exception.class)
    public StudentWaterElectricityResponse refreshWaterElectricity(String stuNum, Long dormId) {
        StudentContextService.StudentDormContext context =
                studentContextService.getRequiredStudentDormContext(stuNum, dormId);
        StudentBase student = context.student();
        Long resolvedDormId = context.dormId();

        DormFee dormFee = dormFeeAccountService.getOrCreateForUpdate(resolvedDormId);

        LocalDateTime operateTime = LocalDateTime.now();
        DormFeeHistory latestElectricAnchor = findLatestAnchorRecord(resolvedDormId, FEE_TYPE_ELECTRIC, operateTime);
        boolean waterBillingEnabled = isBillingEnabled(FEE_TYPE_WATER);
        DormFeeHistory latestWaterAnchor = waterBillingEnabled ? findLatestAnchorRecord(resolvedDormId, FEE_TYPE_WATER, operateTime) : null;
        BigDecimal electricBalanceBeforeRefresh = dormFee.getElectricityBalance();
        BigDecimal waterBalanceBeforeRefresh = dormFee.getWaterBalance();

        insertHistory(student, resolvedDormId, FEE_TYPE_ELECTRIC, OPERATION_REFRESH, PAY_TYPE_SYSTEM,
                BigDecimal.ZERO, dormFee.getElectricityBalance(), operateTime, student.getStuNum());
        if (waterBillingEnabled) {
            insertHistory(student, resolvedDormId, FEE_TYPE_WATER, OPERATION_REFRESH, PAY_TYPE_SYSTEM,
                    BigDecimal.ZERO, dormFee.getWaterBalance(), operateTime, student.getStuNum());
        }

        DormInfo dormInfo = studentDormService.getDormInfo(resolvedDormId);
        RechargeScoreResult electricScoreResult = addCarbonScoreOnAnchorEvent(
                dormInfo,
                FEE_TYPE_ELECTRIC,
                latestElectricAnchor,
                electricBalanceBeforeRefresh,
                operateTime,
                student.getStudentId()
        );
        RechargeScoreResult waterScoreResult = addCarbonScoreOnAnchorEvent(
                dormInfo,
                FEE_TYPE_WATER,
                latestWaterAnchor,
                waterBalanceBeforeRefresh,
                operateTime,
                student.getStudentId()
        );

        StudentBase refreshedStudent = studentContextService.getRequiredStudent(stuNum);
        DormInfo refreshedDormInfo = studentDormService.getDormInfo(resolvedDormId);
        StudentWaterElectricityResponse response =
                studentDormService.buildWaterElectricityResponse(refreshedStudent, resolvedDormId, dormFee, refreshedDormInfo);
        response.setCarbonPointsAdded(electricScoreResult.dormPointsAdded() + waterScoreResult.dormPointsAdded());
        response.setPersonalPointsAdded(electricScoreResult.personalPointsAdded() + waterScoreResult.personalPointsAdded());
        return response;
    }

    @Transactional(rollbackFor = Exception.class)
    public StudentPaymentOrderResponse createPaymentOrder(StudentPayRequest request) {
        String feeType = normalizeFeeType(request.getFeeType());
        if (feeType == null) {
            throw new IllegalArgumentException("费用类型不合法");
        }
        assertBillingEnabled(feeType);

        String payType = normalizePayType(request.getPayType());
        if (payType == null) {
            throw new IllegalArgumentException("支付方式不合法");
        }

        StudentContextService.StudentDormContext context =
                studentContextService.getRequiredStudentDormContext(request.getStuNum(), request.getDormId());
        StudentBase student = context.student();
        Long dormId = context.dormId();
        dormFeeAccountService.getOrCreate(dormId);

        String payerAccount = request.getPayerAccount() == null || request.getPayerAccount().isBlank()
                ? student.getStuNum()
                : request.getPayerAccount().trim();
        String orderNo = buildOrderNo();
        String qrCodeContent = buildQrCodeContent(orderNo, request.getAmount(), payType, feeType);
        LocalDateTime createTime = LocalDateTime.now();

        jdbcTemplate.update(
                "INSERT INTO student_payment_order "
                        + "(order_no, student_id, stu_num, dorm_id, fee_type, pay_type, amount, payer_account, status, qr_code_content, create_time) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                orderNo,
                student.getStudentId(),
                student.getStuNum(),
                dormId,
                feeType,
                payType,
                request.getAmount(),
                payerAccount,
                STATUS_PENDING,
                qrCodeContent,
                createTime
        );

        StudentPaymentOrderResponse response = new StudentPaymentOrderResponse();
        response.setOrderNo(orderNo);
        response.setStatus(STATUS_PENDING);
        response.setAmount(scale(request.getAmount(), 2));
        response.setFeeType(feeType);
        response.setPayType(payType);
        response.setQrCodeContent(qrCodeContent);
        response.setQrCodeImageUrl(buildQrCodeImageUrl(qrCodeContent));
        response.setCreateTime(createTime);
        return response;
    }

    public StudentPaymentOrderResponse getPaymentOrder(String orderNo, String stuNum) {
        StudentBase student = studentContextService.getRequiredStudent(stuNum);
        List<StudentPaymentOrderResponse> orders = jdbcTemplate.query(
                "SELECT order_no, status, amount, fee_type, pay_type, qr_code_content, third_trade_no, create_time, paid_time "
                        + "FROM student_payment_order WHERE order_no = ? AND student_id = ? LIMIT 1",
                (rs, rowNum) -> {
                    StudentPaymentOrderResponse response = new StudentPaymentOrderResponse();
                    response.setOrderNo(rs.getString("order_no"));
                    response.setStatus(rs.getString("status"));
                    response.setAmount(rs.getBigDecimal("amount"));
                    response.setFeeType(rs.getString("fee_type"));
                    response.setPayType(rs.getString("pay_type"));
                    response.setQrCodeContent(rs.getString("qr_code_content"));
                    response.setThirdTradeNo(rs.getString("third_trade_no"));
                    Timestamp createTime = rs.getTimestamp("create_time");
                    response.setCreateTime(createTime == null ? null : createTime.toLocalDateTime());
                    Timestamp paidTime = rs.getTimestamp("paid_time");
                    response.setPaidTime(paidTime == null ? null : paidTime.toLocalDateTime());
                    return response;
                },
                orderNo,
                student.getStudentId()
        );
        if (orders.isEmpty()) {
            throw new IllegalArgumentException("未找到对应支付订单");
        }

        StudentPaymentOrderResponse response = orders.get(0);
        response.setQrCodeImageUrl(buildQrCodeImageUrl(response.getQrCodeContent()));
        return response;
    }

    @Transactional(rollbackFor = Exception.class)
    public StudentPaymentOrderResponse simulatePaymentSuccess(String orderNo, String stuNum) {
        StudentBase student = studentContextService.getRequiredStudent(stuNum);
        StudentPaymentOrderResponse currentOrder = getPaymentOrder(orderNo, stuNum);
        if (STATUS_SUCCESS.equalsIgnoreCase(currentOrder.getStatus())) {
            return currentOrder;
        }
        if (!STATUS_PENDING.equalsIgnoreCase(currentOrder.getStatus())) {
            throw new IllegalStateException("当前订单状态不允许模拟支付");
        }

        List<StudentPayRequest> requests = jdbcTemplate.query(
                "SELECT dorm_id, amount, fee_type, pay_type, payer_account "
                        + "FROM student_payment_order WHERE order_no = ? AND student_id = ? LIMIT 1",
                (rs, rowNum) -> {
                    StudentPayRequest request = new StudentPayRequest();
                    request.setStuNum(stuNum);
                    request.setDormId(rs.getLong("dorm_id"));
                    request.setAmount(rs.getBigDecimal("amount"));
                    request.setFeeType(rs.getString("fee_type"));
                    request.setPayType(rs.getString("pay_type"));
                    request.setPayerAccount(rs.getString("payer_account"));
                    return request;
                },
                orderNo,
                student.getStudentId()
        );
        if (requests.isEmpty()) {
            throw new IllegalArgumentException("未找到对应支付请求");
        }

        String thirdTradeNo = "SIM-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase(Locale.ROOT);
        StudentWaterElectricityResponse settlementResult = applyRecharge(requests.get(0));
        jdbcTemplate.update(
                "UPDATE student_payment_order SET status = ?, third_trade_no = ?, paid_time = ? WHERE order_no = ?",
                STATUS_SUCCESS,
                thirdTradeNo,
                LocalDateTime.now(),
                orderNo
        );
        StudentPaymentOrderResponse response = getPaymentOrder(orderNo, stuNum);
        response.setCarbonPointsAdded(settlementResult.getCarbonPointsAdded());
        response.setPersonalPointsAdded(settlementResult.getPersonalPointsAdded());
        return response;
    }

    private StudentWaterElectricityResponse applyRecharge(StudentPayRequest request) {
        String feeType = normalizeFeeType(request.getFeeType());
        if (feeType == null) {
            throw new IllegalArgumentException("费用类型不合法");
        }
        assertBillingEnabled(feeType);

        String payType = normalizePayType(request.getPayType());
        if (payType == null) {
            throw new IllegalArgumentException("支付方式不合法");
        }

        StudentContextService.StudentDormContext context =
                studentContextService.getRequiredStudentDormContext(request.getStuNum(), request.getDormId());
        StudentBase student = context.student();
        Long dormId = context.dormId();

        DormFee dormFee = dormFeeAccountService.getOrCreateForUpdate(dormId);

        LocalDateTime paymentTime = LocalDateTime.now();
        DormFeeHistory latestAnchor = findLatestAnchorRecord(dormId, feeType, paymentTime);

        BigDecimal balanceBefore = dormFeeAccountService.getBalance(dormFee, feeType);
        BigDecimal balanceAfter = dormFeeAccountService.addBalance(dormFee, feeType, request.getAmount());
        persistDormFee(dormFee);

        String payerAccount = request.getPayerAccount() == null || request.getPayerAccount().isBlank()
                ? student.getStuNum()
                : request.getPayerAccount().trim();
        insertHistory(student, dormId, feeType, OPERATION_RECHARGE, payType,
                request.getAmount(), balanceAfter, paymentTime, payerAccount);

        DormInfo dormInfo = studentDormService.getDormInfo(dormId);
        RechargeScoreResult scoreResult = addCarbonScoreOnAnchorEvent(
                dormInfo,
                feeType,
                latestAnchor,
                balanceBefore,
                paymentTime,
                student.getStudentId()
        );
        StudentBase refreshedStudent = studentContextService.getRequiredStudent(request.getStuNum());
        DormInfo refreshedDormInfo = studentDormService.getDormInfo(dormId);
        StudentWaterElectricityResponse response =
                studentDormService.buildWaterElectricityResponse(refreshedStudent, dormId, dormFee, refreshedDormInfo);
        response.setCarbonPointsAdded(scoreResult.dormPointsAdded());
        response.setPersonalPointsAdded(scoreResult.personalPointsAdded());
        return response;
    }

    private void persistDormFee(DormFee dormFee) {
        if (dormFee == null) {
            return;
        }
        dormFeeMapper.updateById(dormFee);
    }

    private RechargeScoreResult addCarbonScoreOnAnchorEvent(
            DormInfo dormInfo,
            String feeType,
            DormFeeHistory latestAnchor,
            BigDecimal balanceBefore,
            LocalDateTime paymentTime,
            Long currentStudentId
    ) {
        if (dormInfo == null || latestAnchor == null || latestAnchor.getBalanceAfter() == null || latestAnchor.getCreateTime() == null) {
            return new RechargeScoreResult(0, 0);
        }

        BigDecimal consumedAmount = latestAnchor.getBalanceAfter().subtract(dormFeeAccountService.nullSafe(balanceBefore));
        if (consumedAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return new RechargeScoreResult(0, 0);
        }

        long intervalDays = Math.max(Duration.between(latestAnchor.getCreateTime(), paymentTime).toDays(), 1L);
        BigDecimal periodicFee = consumedAmount.divide(BigDecimal.valueOf(intervalDays), 2, RoundingMode.HALF_UP);
        LowCarbonDashboardSupport.ScoreRuleSnapshot scoreRule = lowCarbonRuleService.getActiveScoreRuleSnapshot();
        BigDecimal factor = resolveFeeFactor(feeType, scoreRule);
        BigDecimal score = scoreRule.baseScore().subtract(periodicFee.multiply(factor));
        if (score.compareTo(BigDecimal.ZERO) < 0) {
            score = BigDecimal.ZERO;
        }
        int pointsToAdd = score.setScale(0, RoundingMode.HALF_UP).intValue();
        if (pointsToAdd <= 0) {
            return new RechargeScoreResult(0, 0);
        }

        int currentPoints = dormInfo.getCarbonScore() == null ? 0 : Math.max(dormInfo.getCarbonScore(), 0);
        dormInfo.setCarbonScore(currentPoints + pointsToAdd);
        dormInfoMapper.updateById(dormInfo);
        int personalPointsAdded = distributePointsToResidents(dormInfo, pointsToAdd, currentStudentId);
        return new RechargeScoreResult(pointsToAdd, personalPointsAdded);
    }

    private int distributePointsToResidents(DormInfo dormInfo, int pointsToAdd, Long currentStudentId) {
        List<StudentBase> residents = studentContextService.listDormResidents(dormInfo.getDormId());
        if (residents.isEmpty() || pointsToAdd <= 0) {
            return 0;
        }

        int baseShare = pointsToAdd / residents.size();
        int remainder = pointsToAdd % residents.size();
        int currentStudentAdded = 0;
        for (int index = 0; index < residents.size(); index++) {
            StudentBase resident = residents.get(index);
            int added = baseShare + (index < remainder ? 1 : 0);
            jdbcTemplate.update(
                    "UPDATE student_base SET carbon_score = COALESCE(carbon_score, 0) + ?, update_time = ? WHERE student_id = ?",
                    added,
                    LocalDateTime.now(),
                    resident.getStudentId()
            );
            if (currentStudentId != null && currentStudentId.equals(resident.getStudentId())) {
                currentStudentAdded = added;
            }
        }
        return currentStudentAdded;
    }

    private DormFeeHistory findLatestAnchorRecord(Long dormId, String feeType, LocalDateTime beforeTime) {
        List<DormFeeHistory> histories = findAnchorRecords(dormId, feeType, beforeTime, 1);
        return histories.isEmpty() ? null : histories.get(0);
    }

    private DormFeeHistory findPreviousAnchorRecord(Long dormId, String feeType, DormFeeHistory latestAnchor) {
        if (latestAnchor == null || latestAnchor.getCreateTime() == null) {
            return null;
        }

        List<DormFeeHistory> histories = findAnchorRecords(dormId, feeType, latestAnchor.getCreateTime(), 5);
        for (DormFeeHistory history : histories) {
            if (!isSameHistory(history, latestAnchor)) {
                return history;
            }
        }
        return null;
    }

    private List<DormFeeHistory> findAnchorRecords(Long dormId, String feeType, LocalDateTime beforeTime, int limit) {
        return jdbcTemplate.query(
                "SELECT history_id, student_id, dorm_id, fee_type, operation_type, pay_type, amount, balance_after, create_time, "
                        + "payer_name, payer_stu_num, payer_account "
                        + "FROM student_fee_history "
                        + "WHERE dorm_id = ? AND fee_type = ? "
                        + "AND operation_type IN ('RECHARGE', 'REFRESH') "
                        + "AND create_time <= ? "
                        + "ORDER BY create_time DESC, history_id DESC LIMIT ?",
                this::mapDormFeeHistory,
                dormId,
                feeType,
                beforeTime,
                Math.max(limit, 1)
        );
    }

    private DormFeeHistory mapDormFeeHistory(ResultSet rs, int rowNum) throws SQLException {
        DormFeeHistory history = new DormFeeHistory();
        history.setId(rs.getLong("history_id"));
        history.setStudentId(rs.getLong("student_id"));
        history.setDormId(rs.getLong("dorm_id"));
        history.setFeeType(rs.getString("fee_type"));
        history.setOperationType(rs.getString("operation_type"));
        history.setPayType(rs.getString("pay_type"));
        history.setAmount(rs.getBigDecimal("amount"));
        history.setBalanceAfter(rs.getBigDecimal("balance_after"));
        Timestamp createTime = rs.getTimestamp("create_time");
        history.setCreateTime(createTime == null ? null : createTime.toLocalDateTime());
        history.setPayerName(rs.getString("payer_name"));
        history.setPayerStuNum(rs.getString("payer_stu_num"));
        history.setPayerAccount(rs.getString("payer_account"));
        return history;
    }

    private boolean isSameHistory(DormFeeHistory left, DormFeeHistory right) {
        if (left == null || right == null) {
            return false;
        }
        return left.getId() != null && left.getId().equals(right.getId());
    }

    private BigDecimal resolveFeeFactor(String feeType, LowCarbonDashboardSupport.ScoreRuleSnapshot scoreRule) {
        if (FEE_TYPE_WATER.equals(feeType)) {
            return dormFeeAccountService.nullSafe(scoreRule.waterCarbonFactor());
        }
        return dormFeeAccountService.nullSafe(scoreRule.electricCarbonFactor());
    }

    private String normalizeFeeType(String rawFeeType) {
        if (rawFeeType == null) {
            return null;
        }
        String value = rawFeeType.trim().toUpperCase(Locale.ROOT);
        if (FEE_TYPE_WATER.equals(value)) {
            return FEE_TYPE_WATER;
        }
        if (FEE_TYPE_ELECTRIC.equals(value) || "ELECTRICITY".equals(value)) {
            return FEE_TYPE_ELECTRIC;
        }
        return null;
    }

    private String normalizePayType(String rawPayType) {
        if (rawPayType == null || rawPayType.trim().isEmpty()) {
            return "ALIPAY";
        }
        String value = rawPayType.trim().toUpperCase(Locale.ROOT);
        if ("ALIPAY".equals(value) || "WECHAT".equals(value) || "CASH".equals(value)) {
            return value;
        }
        return null;
    }

    private void insertHistory(
            StudentBase student,
            Long dormId,
            String feeType,
            String operationType,
            String payType,
            BigDecimal amount,
            BigDecimal balanceAfter,
            LocalDateTime createTime,
            String payerAccount
    ) {
        jdbcTemplate.update(
                "INSERT INTO student_fee_history "
                        + "(student_id, dorm_id, fee_type, operation_type, pay_type, amount, balance_after, create_time, payer_name, payer_stu_num, payer_account) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                student.getStudentId(),
                dormId,
                feeType,
                operationType,
                payType,
                scale(amount, 2),
                scale(balanceAfter, 2),
                createTime,
                student.getName(),
                student.getStuNum(),
                safePayerAccount(payerAccount == null || payerAccount.isBlank() ? student.getStuNum() : payerAccount)
        );
        trimDormHistory(dormId);
    }

    private void trimDormHistory(Long dormId) {
        if (dormId == null) {
            return;
        }
        jdbcTemplate.update(
                """
                        DELETE FROM student_fee_history
                        WHERE dorm_id = ?
                          AND history_id NOT IN (
                              SELECT history_id
                              FROM student_fee_history
                              WHERE dorm_id = ?
                              ORDER BY create_time DESC, history_id DESC
                              LIMIT ?
                          )
                        """,
                dormId,
                dormId,
                MAX_HISTORY_RECORDS_PER_DORM
        );
    }

    private String buildOrderNo() {
        String timePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String randomPart = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase(Locale.ROOT);
        return "PAY" + timePart + randomPart;
    }

    private String buildQrCodeContent(String orderNo, BigDecimal amount, String payType, String feeType) {
        return "lowcarbon://pay?orderNo=" + orderNo
                + "&amount=" + scale(amount, 2).toPlainString()
                + "&payType=" + payType
                + "&feeType=" + feeType;
    }

    private String buildQrCodeImageUrl(String qrCodeContent) {
        if (qrCodeContent == null || qrCodeContent.isBlank()) {
            return null;
        }
        return "https://api.qrserver.com/v1/create-qr-code/?size=240x240&data="
                + URLEncoder.encode(qrCodeContent, StandardCharsets.UTF_8);
    }

    private BigDecimal scale(BigDecimal value, int scale) {
        return (value == null ? BigDecimal.ZERO : value).setScale(scale, RoundingMode.HALF_UP);
    }

    private String safePayerAccount(String payerAccount) {
        if (payerAccount == null) {
            return null;
        }
        String trimmed = payerAccount.trim();
        if (trimmed.length() <= 50) {
            return trimmed;
        }
        return trimmed.substring(0, 50);
    }

    private boolean isBillingEnabled(String feeType) {
        UtilityRateConfig rateConfig = utilityRateConfigMapper.selectById(feeType);
        return rateConfig == null || rateConfig.isBillingEnabled();
    }

    private void assertBillingEnabled(String feeType) {
        if (!isBillingEnabled(feeType)) {
            throw new IllegalStateException(feeType + " 已停用计费，无法继续充值");
        }
    }

    private record RechargeScoreResult(int dormPointsAdded, int personalPointsAdded) {
    }
}
