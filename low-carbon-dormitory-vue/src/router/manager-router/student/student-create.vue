<template>
  <section class="dorm-page panel">
    <section class="hero-card">
      <div class="hero-card__head">
        <div>
          <div class="hero-card__eyebrow">宿舍管理 / 基础配置</div>
          <h1>新增学生</h1>
          <p>录入学生基础信息，并同步创建宿舍绑定与初始低碳积分。</p>
        </div>
        <div class="actions">
          <el-button @click="goHome">返回管理首页</el-button>
        </div>
      </div>
    </section>

    <section class="content-card">
      <el-form label-position="top" class="form">
        <div class="grid">
          <el-form-item label="学号">
            <el-input v-model="form.stuNum" placeholder="例如 20240101" />
          </el-form-item>
          <el-form-item label="姓名">
            <el-input v-model="form.name" />
          </el-form-item>
          <el-form-item label="登录密码">
            <el-input v-model="form.password" type="password" show-password />
          </el-form-item>
          <el-form-item label="宿舍楼栋">
            <el-input v-model="form.dormBuilding" placeholder="例如 1号楼" />
          </el-form-item>
          <el-form-item label="宿舍房间">
            <el-input v-model="form.dormRoom" placeholder="例如 302" />
          </el-form-item>
          <el-form-item label="宿舍人数">
            <el-input-number v-model="form.bedTotal" :min="1" :max="12" />
          </el-form-item>
          <el-form-item label="性别（1男 / 0女）">
            <el-input-number v-model="form.gender" :min="0" :max="1" />
          </el-form-item>
          <el-form-item label="学院">
            <el-input v-model="form.college" />
          </el-form-item>
          <el-form-item label="专业">
            <el-input v-model="form.major" />
          </el-form-item>
          <el-form-item label="班级">
            <el-input v-model="form.className" />
          </el-form-item>
          <el-form-item label="年级">
            <el-input v-model="form.grade" />
          </el-form-item>
          <el-form-item label="手机号">
            <el-input v-model="form.phone" />
          </el-form-item>
          <el-form-item label="身份证号">
            <el-input v-model="form.idCard" />
          </el-form-item>
          <el-form-item label="初始低碳积分">
            <el-input-number v-model="form.carbonScore" :min="0" />
          </el-form-item>
        </div>

        <el-button type="primary" :loading="loading" @click="submit">创建学生</el-button>
      </el-form>
    </section>
  </section>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import { createStudentByAdmin } from '@/api/modules/admin'

const router = useRouter()
const loading = ref(false)
const form = reactive({
  stuNum: '',
  name: '',
  password: '',
  dormBuilding: '',
  dormRoom: '',
  bedTotal: 4,
  gender: 1,
  idCard: '',
  phone: '',
  college: '',
  major: '',
  className: '',
  grade: '',
  carbonScore: undefined as number | undefined,
})

function goHome() {
  router.push('/manager/home')
}

async function submit() {
  if (!form.stuNum.trim() || !form.name.trim() || !form.password.trim() || !form.dormBuilding.trim() || !form.dormRoom.trim()) {
    ElMessage.error('请填写学号、姓名、密码、楼栋和房间')
    return
  }

  loading.value = true
  try {
    const { data } = await createStudentByAdmin({
      stuNum: form.stuNum.trim(),
      name: form.name.trim(),
      password: form.password.trim(),
      dormBuilding: form.dormBuilding.trim(),
      dormRoom: form.dormRoom.trim(),
      bedTotal: form.bedTotal,
      gender: form.gender,
      idCard: form.idCard.trim() || undefined,
      phone: form.phone.trim() || undefined,
      college: form.college.trim() || undefined,
      major: form.major.trim() || undefined,
      className: form.className.trim() || undefined,
      grade: form.grade.trim() || undefined,
      carbonScore: form.carbonScore,
    })
    if (data.code !== 200) {
      ElMessage.error(data.msg || '创建学生失败')
      return
    }
    ElMessage.success(`创建成功，学号：${data.data?.stuNum ?? form.stuNum}`)
    form.stuNum = ''
    form.name = ''
    form.password = ''
    form.dormBuilding = ''
    form.dormRoom = ''
    form.bedTotal = 4
    form.idCard = ''
    form.phone = ''
    form.college = ''
    form.major = ''
    form.className = ''
    form.grade = ''
    form.carbonScore = undefined
  } catch (error) {
    console.error(error)
    ElMessage.error('创建学生失败，请稍后重试')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.panel {
  display: grid;
  gap: 18px;
}

.hero-card,
.content-card {
  padding: 24px;
}

.hero-card__head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
}

.hero-card h1 {
  margin: 12px 0 8px;
  color: #244536;
}

.hero-card p {
  margin: 0;
  color: #5f776b;
}

.form {
  display: grid;
  gap: 18px;
}

.grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

@media (max-width: 1000px) {
  .grid {
    grid-template-columns: 1fr;
  }

  .hero-card__head {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
