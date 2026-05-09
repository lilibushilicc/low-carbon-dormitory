package com.example.lowcarbondormitory;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PublicStudentDashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldAllowPublicDashboardQueryByStuNumWithoutToken() throws Exception {
        mockMvc.perform(get("/public/student/low-carbon-dashboard")
                        .param("stuNum", "LC00011"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.currentDormId").isNotEmpty())
                .andExpect(jsonPath("$.data.dorms").isArray());
    }
}
