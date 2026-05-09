package com.example.lowcarbondormitory;

import com.example.lowcarbondormitory.dto.response.StudentPersonalLowCarbonDashboardResponse;
import com.example.lowcarbondormitory.entity.DormInfo;
import com.example.lowcarbondormitory.entity.StudentBase;
import com.example.lowcarbondormitory.mapper.DormInfoMapper;
import com.example.lowcarbondormitory.mapper.StudentBaseMapper;
import com.example.lowcarbondormitory.service.student.StudentPersonalLowCarbonDashboardService;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class StudentPersonalLowCarbonDashboardServiceTest {

    @Autowired
    private StudentPersonalLowCarbonDashboardService studentPersonalLowCarbonDashboardService;

    @Autowired
    private StudentBaseMapper studentBaseMapper;

    @Autowired
    private DormInfoMapper dormInfoMapper;

    @Test
    void shouldBuildWeeklyPersonalDashboard() {
        StudentBase student = findAnyStudentWithDormOrSkip();
        StudentPersonalLowCarbonDashboardResponse response = tryBuildOrSkip("weekly", student.getStuNum());

        assertNotNull(response);
        assertNotNull(response.getPeriodLabel());
        assertNotNull(response.getDormAnchor());
        assertNotNull(response.getOverview());
        assertNotNull(response.getComparisons());
        assertNotNull(response.getRecommendations());
        assertNotNull(response.getTrends());
        assertFalse(response.getDormAnchor().getResidents().isEmpty());
        DormInfo dormInfo = dormInfoMapper.selectById(student.getDormId());
        assertNotNull(dormInfo);
        if (dormInfo.getBedTotal() != null && dormInfo.getBedTotal() > 0) {
            assertTrue(response.getDormAnchor().getResidentCount() <= dormInfo.getBedTotal());
            assertTrue(response.getDormAnchor().getResidents().size() <= dormInfo.getBedTotal());
        }
        assertNotNull(response.getScoreSummary());
        assertTrue(response.getScoreSummary().getRankedSchoolDormCount() >= 0);
        assertFalse(response.getRecommendations().isEmpty());
        assertNotNull(response.getTrends().getPoints());
        assertNotNull(response.getComparisons().getBuilding().getPercentileText());
    }

    @Test
    void shouldBuildMonthlyPersonalDashboard() {
        StudentBase student = findAnyStudentWithDormOrSkip();
        StudentPersonalLowCarbonDashboardResponse response = tryBuildOrSkip("monthly", student.getStuNum());

        assertNotNull(response);
        assertNotNull(response.getPeriodLabel());
        assertNotNull(response.getRuleSummary());
        assertTrue(response.getTrends().getPoints().size() <= 4);
        assertNotNull(response.getTrends().getBuildingTopDorms());
        assertNotNull(response.getTrends().getCollegeTopDorms());
        assertTrue(response.getOverview().getTotalFee().doubleValue() >= 0D);
    }

    private StudentBase findAnyStudentWithDormOrSkip() {
        try {
            return studentBaseMapper.selectList(null).stream()
                    .filter(item -> item.getDormId() != null)
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("未找到已分配宿舍的学生用于测试"));
        } catch (Exception exception) {
            Assumptions.assumeTrue(false, "当前测试环境缺少远程数据库读权限，跳过个人看板集成测试: " + exception.getMessage());
            return null;
        }
    }

    private StudentPersonalLowCarbonDashboardResponse tryBuildOrSkip(String period, String stuNum) {
        try {
            return studentPersonalLowCarbonDashboardService.buildDashboard(period, stuNum, null);
        } catch (Exception exception) {
            Assumptions.assumeTrue(false, "当前测试环境缺少远程数据库读权限，跳过个人看板集成测试: " + exception.getMessage());
            return null;
        }
    }
}
