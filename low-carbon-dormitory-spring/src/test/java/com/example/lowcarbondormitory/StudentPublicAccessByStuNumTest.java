package com.example.lowcarbondormitory;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class StudentPublicAccessByStuNumTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldAllowWaterElectricityQueryWithoutTokenWhenStuNumProvided() throws Exception {
        mockMvc.perform(get("/student/water-electricity")
                        .param("stuNum", "LC00011"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.stuNum").value("LC00011"));
    }

    @Test
    void shouldAllowRewardCenterQueryWithoutTokenWhenStuNumProvided() throws Exception {
        mockMvc.perform(get("/student/rewards")
                        .param("stuNum", "LC00011"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.currentPoints").isNumber());
    }

    @Test
    void shouldAllowProfileQueryWithoutTokenWhenStuNumProvided() throws Exception {
        mockMvc.perform(get("/student/profile")
                        .param("stuNum", "LC00011"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.stuNum").value("LC00011"));
    }

    @Test
    void shouldReachRewardExchangeBusinessLogicWithoutToken() throws Exception {
        mockMvc.perform(post("/student/rewards/exchange")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "stuNum": "LC00011",
                                  "rewardId": 0
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(not(401)));
    }
}
