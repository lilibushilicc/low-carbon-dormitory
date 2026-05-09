package com.example.lowcarbondormitory.service.student;

import com.example.lowcarbondormitory.entity.DormFee;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class DormFeeAccountService {

    private final JdbcTemplate jdbcTemplate;

    public DormFeeAccountService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public DormFee getForUpdate(Long dormId) {
        List<DormFee> fees = jdbcTemplate.query(
                "SELECT dorm_id, electricity_balance, water_balance, last_deduct_time "
                        + "FROM student_dorm_fee WHERE dorm_id = ? FOR UPDATE",
                (rs, rowNum) -> {
                    DormFee fee = new DormFee();
                    fee.setDormId(rs.getLong("dorm_id"));
                    fee.setElectricityBalance(rs.getBigDecimal("electricity_balance"));
                    fee.setWaterBalance(rs.getBigDecimal("water_balance"));
                    Timestamp lastDeductTime = rs.getTimestamp("last_deduct_time");
                    fee.setLastDeductTime(lastDeductTime == null ? null : lastDeductTime.toLocalDateTime());
                    return fee;
                },
                dormId
        );
        return fees.isEmpty() ? null : fees.get(0);
    }

    public DormFee getOrCreateForUpdate(Long dormId) {
        if (dormId == null) {
            return null;
        }

        ensureAccountExists(dormId);
        return getForUpdate(dormId);
    }

    public DormFee getOrCreate(Long dormId) {
        if (dormId == null) {
            return null;
        }

        ensureAccountExists(dormId);
        return jdbcTemplate.query(
                "SELECT dorm_id, electricity_balance, water_balance, last_deduct_time "
                        + "FROM student_dorm_fee WHERE dorm_id = ?",
                (rs, rowNum) -> {
                    DormFee fee = new DormFee();
                    fee.setDormId(rs.getLong("dorm_id"));
                    fee.setElectricityBalance(rs.getBigDecimal("electricity_balance"));
                    fee.setWaterBalance(rs.getBigDecimal("water_balance"));
                    Timestamp lastDeductTime = rs.getTimestamp("last_deduct_time");
                    fee.setLastDeductTime(lastDeductTime == null ? null : lastDeductTime.toLocalDateTime());
                    return fee;
                },
                dormId
        ).stream().findFirst().orElse(null);
    }

    public BigDecimal getBalance(DormFee dormFee, String feeType) {
        if ("WATER".equals(feeType)) {
            return nullSafe(dormFee.getWaterBalance());
        }
        return nullSafe(dormFee.getElectricityBalance());
    }

    public BigDecimal addBalance(DormFee dormFee, String feeType, BigDecimal amount) {
        BigDecimal balanceAfter = getBalance(dormFee, feeType).add(nullSafe(amount));
        setBalance(dormFee, feeType, balanceAfter);
        return balanceAfter;
    }

    public BigDecimal deductBalance(DormFee dormFee, String feeType, BigDecimal amount) {
        BigDecimal balanceAfter = getBalance(dormFee, feeType).subtract(nullSafe(amount));
        if (balanceAfter.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("宿舍余额不足，无法执行扣费");
        }
        setBalance(dormFee, feeType, balanceAfter);
        return balanceAfter;
    }

    public BigDecimal nullSafe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private void ensureAccountExists(Long dormId) {
        jdbcTemplate.update(
                "INSERT INTO student_dorm_fee (dorm_id, electricity_balance, water_balance, last_deduct_time) "
                        + "VALUES (?, 0, 0, NULL) "
                        + "ON CONFLICT (dorm_id) DO NOTHING",
                dormId
        );
    }

    private void setBalance(DormFee dormFee, String feeType, BigDecimal balance) {
        if ("WATER".equals(feeType)) {
            dormFee.setWaterBalance(balance);
            return;
        }
        dormFee.setElectricityBalance(balance);
    }
}
