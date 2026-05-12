<script setup>
import { onMounted, ref } from 'vue'
import { studentMealWeekPageService } from '../api/studentMealWaste.js'
const tableData = ref([])
//搜索表单数据
const searchForm  = ref({
  weekCode: ''
})
//分页数据
const currentPage = ref(1)
const pageSize = ref(10)
const size = ref('default')
const background = ref(false)
const disabled = ref(false)
const total = ref(0)
const handleSizeChange = (val) => {
pageSize.value  = val
currentPage.value = 1

}
const handleCurrentChange = (val) => {
  currentPage.value  = val
   page1()
}
//分页请求

const page1 = async () => {
    const params = {
      page: currentPage.value,
      pageSize: pageSize.value,
      weekCode: searchForm.value.weekCode?.trim() || undefined
    }
    console.log(params)
    const result = await studentMealWeekPageService(params)
      tableData.value = result.data.list || result.data.records || result.data.data || []
      total.value = result.data.total || result.data.count || result.data.totalCount || 0
}
//重置搜索输入框
const resetForm = () => {
  searchForm.value = {
    weekCode: ''
  }
  currentPage.value = 1
   page1()
}
onMounted(() => {
  page1()
})







</script>
<template>
   <div class="S-search">
    <el-form :inline="true" :model="searchForm" class="demo-form-inline">
      <el-form-item label="">
        <el-input
          v-model="searchForm.weekCode"
          placeholder="请输入第几周(weekCode)"
          clearable
        />
      </el-form-item>
      <el-form-item>
        <el-button type="success" @click="page1">搜索</el-button>
        <el-button @click="resetForm">重置</el-button>
      </el-form-item>
    </el-form>
  </div>
    <el-table :data="tableData" style="width: 100%">
      <el-table-column prop="weekCode" label="周编码" width="120" />
      <el-table-column prop="totalWasteWeight" label="总浪费重量" width="140" />
      <el-table-column prop="totalTakeWeight" label="总取餐重量" width="140" />
      <el-table-column prop="totalMeals" label="总用餐次数" width="120" />
      <el-table-column prop="avgWasteRate" label="平均浪费率" width="120" />
      <el-table-column prop="totalCarbonEmission" label="总碳排放" width="120" />
      <el-table-column prop="totalScoreChange" label="总分数变化" width="120" />
  </el-table>
    <el-pagination
      v-model:current-page="currentPage"
      v-model:page-size="pageSize"
      :page-sizes="[10, 20, 30, 40]"
      :size="size"
      :disabled="disabled"
      :background="background"
      layout="total, sizes, prev, pager, next, jumper"
      :total="total"
      @size-change="handleSizeChange"
      @current-change="handleCurrentChange"
    />
</template>
<style scoped>
.container {
  padding: 20px;
}
.top-bar {
  margin-bottom: 20px;
  text-align: right;
}
.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
.form-container {
  padding: 10px 0;
}
.gender-select{
  width: 150px;
}
.input-group {
  display: flex;
  align-items: center;
  gap: 1em;
}
.add-button {
  float: right;
}
</style>
