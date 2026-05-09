<template>
  <div class="page">
    <div class="panel">
      <div class="header">
        <div>
          <h2>个人信息查询</h2>
          <p>优先展示本地缓存，必要时从后端更新。</p>
        </div>
        <el-button @click="goBack">返回首页</el-button>
      </div>

      <el-skeleton :loading="loading" animated :rows="6">
        <template #default>
          <el-alert v-if="errorMessage" :title="errorMessage" type="error" :closable="false" show-icon />

          <div v-else-if="profile" class="section-list">
            <section class="section">
              <h3>基础信息</h3>
              <div class="grid">
                <div class="item"><span>学号</span><strong>{{ profile.stuNum || '-' }}</strong></div>
                <div class="item"><span>姓名</span><strong>{{ profile.name || '-' }}</strong></div>
                <div class="item"><span>登录账号</span><strong>{{ profile.username || '-' }}</strong></div>
                <div class="item"><span>性别</span><strong>{{ formatGender(profile.gender) }}</strong></div>
                <div class="item"><span>手机号</span><strong>{{ profile.phone || '-' }}</strong></div>
                <div class="item"><span>学院</span><strong>{{ profile.college || '-' }}</strong></div>
                <div class="item"><span>专业</span><strong>{{ profile.major || '-' }}</strong></div>
                <div class="item"><span>班级</span><strong>{{ profile.className || '-' }}</strong></div>
                <div class="item"><span>年级</span><strong>{{ profile.grade || '-' }}</strong></div>
              </div>
            </section>

            <section class="section">
              <h3>宿舍信息</h3>
              <div class="grid">
                <div class="item"><span>宿舍号</span><strong>{{ dormLabel }}</strong></div>
                <div class="item"><span>楼栋</span><strong>{{ profile.dormBuilding || '-' }}</strong></div>
                <div class="item"><span>房间</span><strong>{{ profile.dormRoom || '-' }}</strong></div>
                <div class="item"><span>低碳积分</span><strong>{{ profile.carbonScore ?? '-' }}</strong></div>
                <div class="item"><span>宿舍类型</span><strong>{{ profile.dormType || '-' }}</strong></div>
                <div class="item"><span>总床位</span><strong>{{ profile.bedTotal ?? '-' }}</strong></div>
                <div class="item"><span>剩余床位</span><strong>{{ profile.bedAvailable ?? '-' }}</strong></div>
              </div>
            </section>
          </div>
        </template>
      </el-skeleton>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { storeToRefs } from 'pinia'
import type { StudentProfile } from '@/types/student'
import { formatGender } from '@/utils/formatters'
import { requireApiData, resolveErrorMessage } from '@/utils/api-response'
import { useStudentTokenStore } from '@/stores/student-token'
import { fetchStudentProfile } from '@/api/modules/student'

const router = useRouter()
const studentTokenStore = useStudentTokenStore()
const { stuNum, studentInfo, dormLabel } = storeToRefs(studentTokenStore)

const loading = ref(true)
const errorMessage = ref('')
const profile = ref<StudentProfile | null>(studentInfo.value)

function goBack() {
  router.push('/index-student')
}

async function loadProfile() {
  if (!stuNum.value) {
    errorMessage.value = '未检测到登录信息，请重新登录'
    loading.value = false
    return
  }

  try {
    const { data } = await fetchStudentProfile(stuNum.value)
    const profileData = requireApiData(data, '获取个人信息失败')
    profile.value = profileData
    studentTokenStore.updateStudentProfile(profileData)
  } catch (error) {
    console.error('获取个人信息失败:', error)
    errorMessage.value = resolveErrorMessage(error, '服务器异常或网络错误')
  } finally {
    loading.value = false
  }
}

onMounted(loadProfile)
</script>

<style scoped>
.page {
  min-height: 100vh;
  padding: 28px 16px;
  background: linear-gradient(180deg, #eef6f1 0%, #f8fbf9 100%);
}

.panel {
  max-width: 1100px;
  margin: 0 auto;
  padding: 24px;
  border-radius: 20px;
  background: #fff;
  box-shadow: 0 18px 40px rgba(36, 69, 54, 0.1);
}

.header {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 16px;
}

.header h2 {
  margin: 0 0 8px;
  color: #244536;
}

.header p {
  margin: 0;
  color: #5f776b;
}

.section-list {
  display: grid;
  gap: 14px;
}

.section {
  border: 1px solid #e5efe9;
  border-radius: 16px;
  padding: 16px;
  background: #fcfefd;
}

.section h3 {
  margin: 0 0 12px;
  color: #244536;
}

.grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 10px;
}

.item {
  border-radius: 12px;
  background: #eef6f1;
  padding: 12px;
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.item span {
  color: #5f776b;
  font-size: 13px;
}

.item strong {
  color: #1f372c;
}

@media (max-width: 768px) {
  .header {
    flex-direction: column;
  }
}
</style>


