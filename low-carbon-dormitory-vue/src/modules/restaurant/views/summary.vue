<template>
  <div style="padding: 20px">
    <el-card shadow="hover" style="margin-bottom: 20px">
      <div class="query-form" style="display: flex; gap: 16px; align-items: center">

        <!-- 周期类型：只有周、月、学期 -->
        <el-select v-model="query.periodType" placeholder="请选择周期类型" style="width: 150px">
          <el-option label="按周查询" value="week" />
          <el-option label="按月查询" value="month" />
          <el-option label="按学期查询" value="term" />
        </el-select>

        <!-- 年份 -->
        <el-date-picker
          v-model="query.year"
          type="year"
          placeholder="选择年份"
          value-format="YYYY"
          style="width: 150px"
        />

        <!-- 周次 -->
        <el-select
          v-if="query.periodType === 'week'"
          v-model="query.periodValue"
          placeholder="选择周次"
          style="width: 150px"
        >
          <el-option v-for="i in 52" :key="i" :label="`第${i}周`" :value="i" />
        </el-select>

        <!-- 月份 -->
        <el-select
          v-if="query.periodType === 'month'"
          v-model="query.periodValue"
          placeholder="选择月份"
          style="width: 150px"
        >
          <el-option v-for="i in 12" :key="i" :label="`${i}月`" :value="i" />
        </el-select>

        <!-- 学期：1=春季 2=秋季 -->
        <el-select
          v-if="query.periodType === 'term'"
          v-model="query.periodValue"
          placeholder="选择学期"
          style="width: 150px"
        >
          <el-option label="春季学期" value="1" />
          <el-option label="秋季学期" value="2" />
        </el-select>

        <el-button type="primary" @click="loadData">查询</el-button>
      </div>
    </el-card>

    <el-card shadow="hover" style="height: 500px">
      <div ref="chartRef" style="width: 100%; height: 450px"></div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import * as echarts from 'echarts'
import request from '../util/request.js'

const chartRef = ref(null)
let chart = null

// 查询参数
const query = ref({
  periodType: 'week',
  year: new Date().getFullYear().toString(),
  periodValue: '1'
})

// 初始化图表
onMounted(async () => {
  await nextTick()
  chart = echarts.init(chartRef.value)
  loadData()
})

// 请求后端 /summary
const loadData = async () => {
  const { data } = await request.post('/summary', query.value)
  
  chart.setOption({
    title: { text: '各学院碳排放量统计', left: 'center' },
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    xAxis: {
      type: 'category',
      data: data.map(i => i.orgName),
      axisLabel: { interval: 0, rotate: 30 }
    },
    yAxis: { type: 'value', name: '碳排放量（kg）' },
    series: [{
      type: 'bar',
      data: data.map(i => i.totalCarbonEmission),
      itemStyle: { color: '#409eff' }
    }]
  })
}
</script>