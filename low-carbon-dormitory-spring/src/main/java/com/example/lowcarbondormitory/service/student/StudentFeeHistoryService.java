package com.example.lowcarbondormitory.service.student;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.lowcarbondormitory.dto.response.FeeHistoryPageResponse;
import com.example.lowcarbondormitory.entity.DormFeeHistory;
import com.example.lowcarbondormitory.entity.StudentBase;
import com.example.lowcarbondormitory.mapper.StudentBaseMapper;
import com.example.lowcarbondormitory.mapper.DormFeeHistoryMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class StudentFeeHistoryService {

    private static final int MAX_PAGE_SIZE = 10;
    private static final int MAX_HISTORY_RECORDS_PER_DORM = 30;

    @Autowired
    private StudentContextService studentContextService;

    @Autowired
    private DormFeeHistoryMapper dormFeeHistoryMapper;

    @Autowired
    private StudentBaseMapper studentBaseMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public FeeHistoryPageResponse getFeeHistory(String stuNum, Long dormId, Integer pageNum, Integer pageSize) {
        if ((stuNum == null || stuNum.trim().isEmpty()) && dormId == null) {
            throw new IllegalArgumentException("鐎涳箑褰块幋鏍ь問閼稿秴褰挎稉宥堝厴閸氬本妞傛稉铏光敄");
        }

        int safePageNum = Math.max(pageNum == null ? 1 : pageNum, 1);
        int safePageSize = Math.min(Math.max(pageSize == null ? MAX_PAGE_SIZE : pageSize, 1), MAX_PAGE_SIZE);

        StudentBase student = null;
        if (stuNum != null && !stuNum.trim().isEmpty()) {
            student = studentContextService.getRequiredStudent(stuNum);
        }

        if (dormId != null) {
            trimDormHistory(dormId);
        }

        LambdaQueryWrapper<DormFeeHistory> queryWrapper = new LambdaQueryWrapper<DormFeeHistory>()
                .select(
                        DormFeeHistory::getId,
                        DormFeeHistory::getStudentId,
                        DormFeeHistory::getDormId,
                        DormFeeHistory::getFeeType,
                        DormFeeHistory::getOperationType,
                        DormFeeHistory::getPayType,
                        DormFeeHistory::getAmount,
                        DormFeeHistory::getBalanceAfter,
                        DormFeeHistory::getCreateTime,
                        DormFeeHistory::getPayerName,
                        DormFeeHistory::getPayerStuNum,
                        DormFeeHistory::getPayerAccount
                )
                .orderByDesc(DormFeeHistory::getId);
        if (dormId != null) {
            queryWrapper.eq(DormFeeHistory::getDormId, dormId);
        } else if (student != null) {
            queryWrapper.eq(DormFeeHistory::getStudentId, student.getStudentId());
        }

        Page<DormFeeHistory> page = dormFeeHistoryMapper.selectPage(
                new Page<>(safePageNum, safePageSize),
                queryWrapper
        );
        List<DormFeeHistory> records = page.getRecords();
        fillPayerInfo(records);

        FeeHistoryPageResponse response = new FeeHistoryPageResponse();
        response.setRecords(records);
        response.setTotal(page.getTotal());
        response.setPages(page.getPages());
        response.setCurrent(page.getCurrent());
        return response;
    }

    private void trimDormHistory(Long dormId) {
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

    private void fillPayerInfo(List<DormFeeHistory> records) {
        if (records == null || records.isEmpty()) {
            return;
        }

        Set<Long> payerIds = records.stream()
                .map(DormFeeHistory::getStudentId)
                .filter(id -> id != null && id > 0)
                .collect(Collectors.toSet());
        if (payerIds.isEmpty()) {
            return;
        }

        Map<Long, StudentBase> payerById = studentBaseMapper.selectBatchIds(payerIds).stream()
                .filter(student -> student != null && student.getStudentId() != null)
                .collect(Collectors.toMap(
                        StudentBase::getStudentId,
                        student -> student,
                        (left, right) -> left,
                        HashMap::new
                ));

        for (DormFeeHistory record : records) {
            if (record == null || record.getStudentId() == null) {
                continue;
            }
            StudentBase payer = payerById.get(record.getStudentId());
            if (payer == null) {
                continue;
            }
            if (record.getPayerName() == null || record.getPayerName().isBlank()) {
                record.setPayerName(payer.getName());
            }
            if (record.getPayerStuNum() == null || record.getPayerStuNum().isBlank()) {
                record.setPayerStuNum(payer.getStuNum());
            }
            if (record.getPayerAccount() == null || record.getPayerAccount().isBlank()) {
                record.setPayerAccount(payer.getStuNum());
            }
        }
    }
}

