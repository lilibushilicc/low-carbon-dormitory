<template>
  <div class="login">
    <div class="login-box">
      <div class="login-title">低碳宿舍管理系统</div>

      <el-radio-group v-model="role" class="role-switch">
        <el-radio-button value="student">学生登录</el-radio-button>
        <el-radio-button value="admin">管理员登录</el-radio-button>
      </el-radio-group>

      <el-form
        ref="loginFormRef"
        :model="loginForm"
        :rules="loginRules"
        label-width="80px"
        @keyup.enter="handleLogin"
      >
        <el-form-item :label="role === 'student' ? '学号' : '账号'" prop="username">
          <el-input
            v-model="loginForm.username"
            :placeholder="role === 'student' ? '请输入学号' : '请输入管理员账号'"
            clearable
          />
        </el-form-item>

        <el-form-item label="密码" prop="password">
          <el-input
            v-model="loginForm.password"
            type="password"
            placeholder="请输入密码"
            show-password
            clearable
          />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" class="login-btn" :loading="loading" @click="handleLogin">
            {{ role === 'student' ? '学生登录' : '管理员登录' }}
          </el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useRoute, useRouter } from 'vue-router'
import type { FormInstance, FormRules } from 'element-plus'
import { useStudentTokenStore } from '@/stores/student-token'
import { loginStudent } from '@/api/modules/student'
import { adminLogin } from '@/api/modules/admin'
import { useAdminTokenStore } from '@/stores/admin-token'
import { getDefaultHomePath } from '@/utils/app-navigation'

interface LoginForm {
  username: string
  password: string
}

const router = useRouter()
const route = useRoute()
const studentTokenStore = useStudentTokenStore()
const adminTokenStore = useAdminTokenStore()
const loginFormRef = ref<FormInstance>()
const loading = ref(false)
const role = ref<'student' | 'admin'>(route.query.role === 'admin' ? 'admin' : 'student')

const loginForm = reactive<LoginForm>({
  username: '',
  password: '',
})

const loginRules: FormRules<LoginForm> = {
  username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

async function handleLogin() {
  const form = loginFormRef.value
  if (!form || loading.value) {
    return
  }

  const valid = await form.validate().catch(() => false)
  if (!valid) {
    return
  }

  loading.value = true

  try {
    if (role.value === 'student') {
      const { data } = await loginStudent(loginForm.username.trim(), loginForm.password)
      if (data.code !== 200 || !data.data?.studentInfo) {
        ElMessage.error(data.msg || '学生登录失败')
        return
      }
      if (!data.data.token) {
        ElMessage.error('登录响应缺少 token，请重启后端服务后再试')
        return
      }

      adminTokenStore.clearAdminToken()
      localStorage.removeItem('loginUser')
      studentTokenStore.setStudentToken(data.data.studentInfo, data.data.stuNum, data.data.token)
      ElMessage.success(data.msg || '学生登录成功')
      await router.replace(getDefaultHomePath('student'))
      return
    }

    const { data } = await adminLogin(loginForm.username.trim(), loginForm.password)
    if (data.code !== 200 || !data.data) {
      ElMessage.error(data.msg || '管理员登录失败')
      return
    }
    if (!data.data.token) {
      ElMessage.error('登录响应缺少 token，请重启后端服务后再试')
      return
    }

    studentTokenStore.clearStudentToken()
    adminTokenStore.setAdminToken(data.data)
    localStorage.setItem(
      'loginUser',
      JSON.stringify({
        token: data.data.token,
        username: data.data.username,
        displayName: data.data.displayName || data.data.username,
        adminId: data.data.adminId || 0,
        role: 'ADMIN',
      }),
    )
    ElMessage.success(data.msg || '管理员登录成功')
    await router.replace(getDefaultHomePath('admin'))
  } catch (error) {
    console.error('登录请求失败:', error)
    ElMessage.error('服务器异常或网络错误')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login {
  width: 100vw;
  min-height: 100vh;
  background:
    radial-gradient(circle at 15% 10%, rgba(128, 186, 152, 0.25), transparent 38%),
    radial-gradient(circle at 88% 86%, rgba(86, 138, 112, 0.2), transparent 42%),
    linear-gradient(140deg, #e9f4ee 0%, #dcecdf 48%, #edf7f1 100%);
  display: flex;
  justify-content: center;
  align-items: center;
  padding: clamp(16px, 3vw, 32px);
}

.login-box {
  width: min(520px, 100%);
  padding: clamp(22px, 3.2vw, 36px);
  background: #ffffff;
  border-radius: 22px;
  border: 1px solid rgba(53, 101, 76, 0.12);
  box-shadow:
    0 24px 54px rgba(39, 80, 60, 0.15),
    0 8px 18px rgba(39, 80, 60, 0.08);
  backdrop-filter: blur(2px);
}

.login-title {
  text-align: center;
  margin-bottom: 20px;
  font-size: clamp(24px, 3.6vw, 32px);
  font-weight: 700;
  color: #244536;
  line-height: 1.25;
  letter-spacing: 0.01em;
  font-family: 'PingFang SC', 'Noto Sans SC', 'Microsoft YaHei', sans-serif;
}

.role-switch {
  width: 100%;
  margin-bottom: 18px;
  display: grid;
  grid-template-columns: 1fr 1fr;
  background: #f2f8f4;
  border-radius: 12px;
  padding: 3px;
}

.role-switch :deep(.el-radio-button__inner) {
  width: 100%;
  border-radius: 10px !important;
  border: none !important;
  background: transparent;
  color: #2f5141;
  font-weight: 600;
  letter-spacing: 0.01em;
  box-shadow: none !important;
}

.role-switch :deep(.el-radio-button__original-radio:checked + .el-radio-button__inner) {
  background: linear-gradient(135deg, #2f8f68 0%, #2a7b59 100%);
  color: #fff;
}

:deep(.el-form-item) {
  margin-bottom: 18px;
}

:deep(.el-form-item__label) {
  color: #2b4c3d;
  font-weight: 600;
}

:deep(.el-input__wrapper) {
  border-radius: 12px;
  background: #f8fbf9;
  box-shadow: 0 0 0 1px #d7e4dd inset !important;
  transition: box-shadow 0.16s ease;
}

:deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px #8ab8a0 inset !important;
}

:deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1.5px #2f8f68 inset !important;
}

.login-btn {
  width: 100%;
  height: 46px;
  border-radius: 12px;
  border: none;
  font-size: 15px;
  font-weight: 700;
  letter-spacing: 0.02em;
  background: linear-gradient(135deg, #2f8f68 0%, #2a7b59 100%);
  box-shadow: 0 10px 22px rgba(43, 120, 84, 0.24);
  transition: transform 0.16s ease, box-shadow 0.16s ease;
}

.login-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 14px 26px rgba(43, 120, 84, 0.3);
}

@media (max-width: 640px) {
  .login-box {
    border-radius: 16px;
    padding: 18px;
  }

  .login-title {
    margin-bottom: 16px;
  }

  :deep(.el-form-item__label) {
    padding-bottom: 6px;
  }
}
</style>
