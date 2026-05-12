<script setup>
import { onMounted, ref, reactive } from 'vue'
import { ElMessage, ElForm } from 'element-plus'
import { studentMealPageService } from '../api/studentMealWaste.js'
const tableData = ref([])
//搜索表单数据
const searchForm  = ref({
  studentId: '',
  mealDate: ''
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
      studentId: searchForm.value.studentId?.trim()||undefined,
      mealDate: searchForm.value.mealDate || undefined
    }
    console.log(params)
    const result = await studentMealPageService(params)
      tableData.value = result.data.list || result.data.records || result.data.data || []
      total.value = result.data.total || result.data.count || result.data.totalCount || 0
}
//重置搜索输入框
const resetForm = () => {
  searchForm.value = {
    studentId: '',
    mealDate: ''
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
            v-model="searchForm.studentId"
            placeholder="请输入学号"
            clearable
          />
      </el-form-item>
     <el-form-item label="日期">
            <el-date-picker
            v-model="searchForm.mealDate"
            type="date"
            placeholder="选择用餐日期"
            format="YYYY-MM-DD"
            value-format="YYYY-MM-DD"
          />

</el-form-item>
      <el-form-item>
        <el-button type="success" @click="page1">搜索</el-button>
        <el-button @click="resetForm">重置</el-button>
      </el-form-item>
    </el-form>
  </div>
    <el-table :data="tableData" style="width: 100%">
      <el-table-column prop="wasteId" label="id" width="50" />
        <el-table-column prop="studentId" label="学生"width="60" />
        <el-table-column prop="takeWeight" label="取餐重量"  width="85"/>
        <el-table-column prop="leftWeight" label="饭后剩余" width="85"/>
        <el-table-column prop="wasteWeight" label="浪费的重量" width="100" />
        <el-table-column prop="wasteRate" label="浪费率" width="70"/>
        <el-table-column prop="mealDate" label="用餐日期" width="100"/>
        <el-table-column prop="carbonEmission" label="折合碳排放"width="100" />
        <el-table-column prop="scoreChange" label="分数变化"width="100" />
        <el-table-column prop="createTime" label="创建时间" width="160"/>
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
