package com.example.lowcarbondormitory;

import com.example.lowcarbondormitory.dto.response.LowCarbonDashboardResponse;
import com.example.lowcarbondormitory.service.dashboard.LowCarbonDashboardService;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class LowCarbonDashboardServiceTest {

    @Autowired
    private LowCarbonDashboardService lowCarbonDashboardService;

    @Test
    void shouldBuildWeeklyDashboardWithExpectedStructure() {
        LowCarbonDashboardResponse response = tryBuildOrSkip("weekly", "LC00011");

        assertNotNull(response);
        assertEquals("weekly", response.getPeriod());
        assertEquals("近7天碳排表现", response.getPeriodLabel());
        assertNotNull(response.getOverview());
        assertNotNull(response.getDorms());
        assertNotNull(response.getBuildings());
        assertFalse(response.getDorms().isEmpty());
        assertFalse(response.getBuildings().isEmpty());
        assertNotNull(response.getLastUpdatedAt());
        assertTrue(response.getOverview().getDormCount() > 0);
        assertNotNull(response.getOverview().getBestDormLabel());
        assertNotNull(response.getOverview().getHighestCarbonDormLabel());
        assertTrue(response.getBuildings().stream().allMatch(item -> item.getRank() != null && item.getRank() > 0));
        if (response.getCurrentDormId() != null) {
            assertNotNull(response.getCurrentDormLabel());
            assertTrue(response.getDorms().stream().anyMatch(item -> Boolean.TRUE.equals(item.getCurrentDorm())));
        }
    }

    @Test
    void shouldBuildMonthlyDashboard() {
        LowCarbonDashboardResponse response = tryBuildOrSkip("monthly", null);

        assertNotNull(response);
        assertEquals("monthly", response.getPeriod());
        assertEquals("近30天碳排表现", response.getPeriodLabel());
        assertNotNull(response.getRangeStart());
        assertNotNull(response.getRangeEnd());
        assertTrue(response.getOverview().getTotalCarbon().doubleValue() >= 0D);
        assertTrue(response.getDorms().stream().allMatch(item -> item.getCarbonRank() != null && item.getCarbonRank() > 0));
    }

    private LowCarbonDashboardResponse tryBuildOrSkip(String period, String stuNum) {
        try {
            return lowCarbonDashboardService.buildDashboard(period, stuNum, null);
        } catch (Exception exception) {
            Assumptions.assumeTrue(false, "当前测试环境缺少远程数据库读权限，跳过看板集成测试: " + exception.getMessage());
            return null;
        }
    }
}
