<template>
  <el-card class="meal-record-card" shadow="hover">
    <template #header>
      <div class="card-header">
        <span class="title">学生取餐记录</span>
      </div>
    </template>

    <el-form :model="form" :rules="rules" ref="formRef" label-width="100px">
      <el-form-item label="学生ID" prop="studentId">
        <el-input-number v-model="form.studentId" :min="1" controls-position="right" placeholder="请输入学生ID" />
      </el-form-item>

      <el-form-item label="取餐重量(kg)" prop="takeWeight">
        <el-input v-model.number="form.takeWeight" type="number" step="0.01" placeholder="请输入取餐重量" />
      </el-form-item>

      <el-form-item label="剩余重量(kg)" prop="leftWeight">
        <el-input v-model.number="form.leftWeight" type="number" step="0.01" placeholder="请输入剩余重量" />
      </el-form-item>

      <el-form-item>
        <el-button type="primary" @click="submitForm" :loading="loading">提交记录</el-button>
        <el-button @click="resetForm">重置</el-button>
      </el-form-item>
    </el-form>
  </el-card>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import request from '../util/request.js'

const formRef = ref(null)
const loading = ref(false)

const form = reactive({
  studentId: null,
  takeWeight: null,
  leftWeight: null
})

const rules = reactive({
  studentId: [
    { required: true, message: '请输入学生ID', trigger: 'blur' }
  ],
  takeWeight: [
    { required: true, message: '请输入取餐重量', trigger: 'blur' },
    { type: 'number', min: 0.01, message: '取餐重量必须大于0', trigger: 'blur' }
  ],
  leftWeight: [
    { required: true, message: '请输入剩余重量', trigger: 'blur' },
    { type: 'number', min: 0, message: '剩余重量不能为负数', trigger: 'blur' }
  ]
})

const submitForm = async () => {
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    loading.value = true
    try {
      await request.post('/student/meal/add', form)
      ElMessage.success('提交成功')
      resetForm()
    } catch (err) {
      ElMessage.error(err.response?.data?.msg || '提交失败')
    } finally {
      loading.value = false
    }
  })
}

const resetForm = () => {
  formRef.value.resetFields()
}
</script>

<style scoped>
.meal-record-card {
  max-width: 500px;
  margin: 2rem auto;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.title {
  font-size: 18px;
  font-weight: 600;
}
</style>