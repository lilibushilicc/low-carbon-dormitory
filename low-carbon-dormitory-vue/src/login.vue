<template>
  <div class="login-shell">
    <div class="login-haze login-haze--left"></div>
    <div class="login-haze login-haze--right"></div>

    <section class="login-panel">
      <div class="login-copy">
        <p class="login-copy__eyebrow">校园低碳管理平台</p>
        <h1>统一登录</h1>
      </div>

      <div class="login-switch" role="tablist" aria-label="登录角色切换">
        <button
          type="button"
          class="login-switch__item"
          :class="{ 'is-active': role === 'student' }"
          @click="selectRole('student')"
        >
          学生登录
        </button>
        <button
          type="button"
          class="login-switch__item"
          :class="{ 'is-active': role === 'admin' }"
          @click="selectRole('admin')"
        >
          管理员登录
        </button>
      </div>

      <el-form
        ref="loginFormRef"
        :model="loginForm"
        :rules="loginRules"
        class="login-form"
        label-position="top"
        @keyup.enter="handleLogin"
      >
        <el-form-item prop="username">
          <template #label>
            <span class="field-label">{{ usernameLabel }}</span>
          </template>
          <el-input
            v-model="loginForm.username"
            :placeholder="usernamePlaceholder"
            size="large"
            clearable
          />
        </el-form-item>

        <el-form-item prop="password">
          <template #label>
            <span class="field-label">密码</span>
          </template>
          <el-input
            v-model="loginForm.password"
            type="password"
            placeholder="请输入密码"
            size="large"
            show-password
            clearable
          />
        </el-form-item>

        <el-form-item class="login-form__action">
          <el-button type="primary" class="login-submit" :loading="loading" @click="handleLogin">
            {{ submitLabel }}
          </el-button>
        </el-form-item>
      </el-form>

      <div class="login-shortcuts">
        <button type="button" class="login-shortcuts__link" @click="goToStudentHome">
          进入学生端
        </button>
        <button type="button" class="login-shortcuts__link" @click="goToAdminHome">
          进入管理端
        </button>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
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

const usernameLabel = computed(() => (role.value === 'student' ? '学号' : '账号'))
const usernamePlaceholder = computed(() =>
  role.value === 'student' ? '请输入学号' : '请输入管理员账号',
)
const submitLabel = computed(() =>
  role.value === 'student' ? '登录并进入学生端' : '登录并进入管理端',
)

const loginRules: FormRules<LoginForm> = {
  username: [{ required: true, message: '请输入账号信息', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

function selectRole(nextRole: 'student' | 'admin') {
  if (loading.value || role.value === nextRole) {
    return
  }
  role.value = nextRole
}

function goToStudentHome() {
  router.push(getDefaultHomePath('student'))
}

function goToAdminHome() {
  router.push(getDefaultHomePath('admin'))
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
.login-shell {
  --login-green: #3f8a63;
  --login-green-deep: #234939;
  --login-line: rgba(79, 120, 96, 0.18);
  --login-card-shadow: 0 32px 90px rgba(47, 88, 65, 0.12);
  position: relative;
  min-height: 100vh;
  overflow: hidden;
  display: grid;
  place-items: center;
  padding: 28px 18px;
  background:
    radial-gradient(circle at 16% 22%, rgba(148, 209, 182, 0.22), transparent 26%),
    radial-gradient(circle at 80% 78%, rgba(231, 217, 165, 0.2), transparent 30%),
    linear-gradient(135deg, #edf7f1 0%, #eef6f1 42%, #f6faee 100%);
}

.login-haze {
  position: absolute;
  inset: auto;
  width: 32vw;
  min-width: 260px;
  max-width: 520px;
  aspect-ratio: 1;
  border-radius: 999px;
  filter: blur(68px);
  opacity: 0.38;
  pointer-events: none;
}

.login-haze--left {
  left: -8vw;
  top: 16vh;
  background: rgba(160, 221, 191, 0.78);
}

.login-haze--right {
  right: -10vw;
  bottom: 12vh;
  background: rgba(233, 219, 164, 0.68);
}

.login-panel {
  position: relative;
  z-index: 1;
  width: min(540px, 100%);
  padding: clamp(20px, 3vw, 32px);
  border-radius: 18px;
  border: 1px solid rgba(255, 255, 255, 0.68);
  background: rgba(255, 255, 255, 0.92);
  box-shadow: var(--login-card-shadow);
  backdrop-filter: blur(12px);
}

.login-copy {
  display: grid;
  gap: 4px;
  margin-bottom: 18px;
}

.login-copy__eyebrow {
  margin: 0;
  color: #557865;
  font-size: 13px;
  font-weight: 800;
  letter-spacing: 0.02em;
}

.login-copy h1 {
  margin: 0;
  color: var(--login-green-deep);
  font-size: clamp(36px, 6vw, 56px);
  line-height: 1.02;
  letter-spacing: -0.05em;
  font-weight: 900;
  font-family: 'Microsoft YaHei', 'PingFang SC', 'Noto Sans SC', sans-serif;
}

.login-switch {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
  margin-bottom: 20px;
}

.login-switch__item {
  min-height: 46px;
  border-radius: 12px;
  border: 1px solid #d8e5de;
  background: rgba(255, 255, 255, 0.82);
  color: #335845;
  font: inherit;
  font-size: 14px;
  font-weight: 800;
  cursor: pointer;
  transition:
    transform 0.18s ease,
    border-color 0.18s ease,
    background 0.18s ease,
    box-shadow 0.18s ease;
}

.login-switch__item:hover {
  transform: translateY(-1px);
}

.login-switch__item.is-active {
  border-color: transparent;
  color: #fff;
  background: linear-gradient(135deg, #438d66 0%, #347b57 100%);
  box-shadow: 0 16px 34px rgba(55, 123, 87, 0.24);
}

.login-form {
  display: grid;
}

.field-label {
  color: #2d4d3d;
  font-size: 13px;
  font-weight: 800;
}

:deep(.el-form-item) {
  margin-bottom: 16px;
}

:deep(.el-form-item__label) {
  padding-bottom: 6px;
}

:deep(.el-input__wrapper) {
  min-height: 46px;
  padding: 0 12px;
  border-radius: 12px;
  background: #fff;
  box-shadow: 0 0 0 1px var(--login-line) inset !important;
  transition: box-shadow 0.18s ease, transform 0.18s ease;
}

:deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px rgba(74, 142, 104, 0.34) inset !important;
}

:deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1.5px rgba(63, 138, 99, 0.76) inset !important;
}

:deep(.el-input__inner) {
  color: #294737;
  font-size: 13px;
}

:deep(.el-input__inner::placeholder) {
  color: #9bad9f;
}

.login-form__action {
  margin-top: 2px;
  margin-bottom: 12px;
}

.login-submit {
  width: 100%;
  min-height: 48px;
  border: none;
  border-radius: 12px;
  font-size: 15px;
  font-weight: 900;
  letter-spacing: 0.01em;
  background: linear-gradient(135deg, #438d66 0%, #357c58 100%);
  box-shadow: 0 18px 36px rgba(56, 125, 88, 0.24);
  transition: transform 0.18s ease, box-shadow 0.18s ease;
}

.login-submit:hover {
  transform: translateY(-1px);
  box-shadow: 0 20px 38px rgba(56, 125, 88, 0.28);
}

.login-shortcuts {
  display: flex;
  justify-content: center;
  gap: 18px;
  flex-wrap: wrap;
}

.login-shortcuts__link {
  border: none;
  background: transparent;
  color: #3b654f;
  font: inherit;
  font-size: 13px;
  font-weight: 800;
  cursor: pointer;
  transition: color 0.18s ease, transform 0.18s ease;
}

.login-shortcuts__link:hover {
  color: #25533f;
  transform: translateY(-1px);
}

@media (max-width: 720px) {
  .login-shell {
    padding: 16px;
  }

  .login-panel {
    width: min(500px, 100%);
    padding: 18px 16px;
    border-radius: 16px;
  }

  .login-copy {
    gap: 4px;
    margin-bottom: 16px;
  }

  .login-switch {
    gap: 8px;
    margin-bottom: 16px;
  }

  .login-switch__item {
    min-height: 44px;
    font-size: 13px;
  }

  :deep(.el-input__wrapper) {
    min-height: 44px;
  }

  .login-submit {
    min-height: 46px;
    font-size: 14px;
  }

  .login-shortcuts {
    gap: 14px;
  }
}
</style>
