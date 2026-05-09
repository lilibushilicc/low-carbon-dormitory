import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAdminTokenStore } from '@/stores/admin-token'
import { useStudentTokenStore } from '@/stores/student-token'
import type { StudentProfile } from '@/types/student'
import { resolvePostLoginPath } from '@/utils/app-navigation'
import { buildUnifiedLoginUrl } from '@/utils/unified-login'
import { parseJsonSafely } from '@/utils/json'

const lazyView = <T>(loader: () => Promise<T>) => loader
type RouteComponent = NonNullable<RouteRecordRaw['component']>

const studentRoute = (path: string, name: string, component: RouteComponent) =>
  ({
    path,
    name,
    component,
    meta: { requiresStudentAuth: true },
  }) as RouteRecordRaw

const adminRoute = (
  path: string,
  name: string,
  component: RouteComponent,
  extra: Partial<RouteRecordRaw> = {},
): RouteRecordRaw =>
  ({
    path,
    name,
    component,
    meta: { requiresAdminAuth: true },
    ...extra,
  }) as RouteRecordRaw

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    name: 'login',
    component: lazyView(() => import('@/login.vue')),
    meta: { guestOnly: true },
  },
  studentRoute('/index-student', 'index-student', lazyView(() => import('@/router/student-router/home/index-student.vue'))),
  studentRoute('/personal-info', 'personal-info', lazyView(() => import('@/router/student-router/profile/personal-info.vue'))),
  studentRoute('/pay-up', 'pay-up', lazyView(() => import('@/router/student-router/billing/pay-up.vue'))),
  studentRoute('/water-electricity', 'water-electricity', lazyView(() => import('@/router/student-router/billing/water-electricity.vue'))),
  studentRoute('/history-fee', 'history-fee', lazyView(() => import('@/router/student-router/billing/history-fee.vue'))),
  studentRoute('/low-carbon-dashboard', 'low-carbon-dashboard', lazyView(() => import('@/router/student-router/dashboard/low-carbon-dashboard.vue'))),
  studentRoute('/reward-exchange', 'reward-exchange', lazyView(() => import('@/router/student-router/reward/reward-exchange.vue'))),
  studentRoute('/reward-exchange-antd', 'reward-exchange-antd', lazyView(() => import('@/router/student-router/reward/reward-exchange-antd.vue'))),
  studentRoute('/low-carbon-rule-readonly', 'low-carbon-rule-readonly', lazyView(() => import('@/router/student-router/rules/low-carbon-rule-readonly.vue'))),

  adminRoute('/manager/home', 'manager-home', lazyView(() => import('@/router/manager-router/home/manager-home.vue'))),
  {
    path: '/manager',
    component: lazyView(() => import('@/router/manager-router/dorm/manager-dorm-shell.vue')),
    meta: { requiresAdminAuth: true },
    children: [
      {
        path: 'low-carbon-overview',
        name: 'manager-low-carbon-overview',
        component: lazyView(() => import('@/router/student-router/dashboard/low-carbon-dashboard-overview.vue')),
        meta: { requiresAdminAuth: true },
        props: { adminMode: true },
      },
      {
        path: 'low-carbon-overview/dorm/:dormId',
        name: 'manager-low-carbon-dorm-detail',
        component: lazyView(() => import('@/router/manager-router/dashboard/manager-low-carbon-dorm-detail.vue')),
        meta: { requiresAdminAuth: true },
      },
      {
        path: 'low-carbon-rule-config',
        name: 'manager-low-carbon-rule-config',
        component: lazyView(() => import('@/router/manager-router/config/low-carbon-rule-config.vue')),
        meta: { requiresAdminAuth: true },
      },
      {
        path: 'student-create',
        name: 'manager-student-create',
        component: lazyView(() => import('@/router/manager-router/student/student-create.vue')),
        meta: { requiresAdminAuth: true },
      },
      {
        path: 'reward-manage',
        name: 'manager-reward-manage',
        component: lazyView(() => import('@/router/manager-router/reward/reward-manage.vue')),
        meta: { requiresAdminAuth: true },
      },
      {
        path: 'dorm-fee-deduct',
        name: 'manager-dorm-fee-deduct',
        component: lazyView(() => import('@/router/manager-router/fee/dorm-fee-deduct.vue')),
        meta: { requiresAdminAuth: true },
      },
    ],
  },
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
})

function resolveRedirectQuery(redirect: unknown, role: 'admin' | 'student') {
  return resolvePostLoginPath(typeof redirect === 'string' ? redirect : '', role)
}

function resolveGuestRedirect(
  requestedRole: string,
  redirect: unknown,
  adminLoggedIn: boolean,
  studentLoggedIn: boolean,
) {
  if (requestedRole === 'admin') {
    return adminLoggedIn ? resolveRedirectQuery(redirect, 'admin') : true
  }

  if (requestedRole === 'student') {
    return studentLoggedIn ? resolveRedirectQuery(redirect, 'student') : true
  }

  if (adminLoggedIn) {
    return resolveRedirectQuery(redirect, 'admin')
  }

  if (studentLoggedIn) {
    return resolveRedirectQuery(redirect, 'student')
  }

  return true
}

function redirectToLogin(fullPath: string, role: 'admin' | 'student', message: string) {
  ElMessage.warning(message)
  window.location.replace(buildUnifiedLoginUrl({ redirectPath: resolvePostLoginPath(fullPath, role), role }))
  return false
}

router.beforeEach((to) => {
  const studentTokenState = useStudentTokenStore()
  const adminTokenState = useAdminTokenStore()
  const nextQuery = { ...to.query }
  const requestedRole = to.query.role === 'admin' ? 'admin' : to.query.role === 'student' ? 'student' : ''
  const ssoMode = typeof to.query.ssoMode === 'string' ? to.query.ssoMode : ''
  const ssoStudentInfo = parseJsonSafely<StudentProfile>(to.query.ssoStudentInfo)
  const ssoAdminProfile = parseJsonSafely<{
    token: string
    adminId: number
    username: string
    displayName: string
  }>(to.query.ssoAdminProfile)
  const ssoStudentToken = typeof to.query.ssoStudentToken === 'string' ? to.query.ssoStudentToken : ''
  const ssoStudentStuNum = typeof to.query.ssoStudentStuNum === 'string' ? to.query.ssoStudentStuNum : ''
  const ssoAdminToken = typeof to.query.ssoAdminToken === 'string' ? to.query.ssoAdminToken : ''
  const ssoLoginUser = parseJsonSafely<Record<string, unknown>>(to.query.ssoLoginUser)

  if (ssoMode === 'student' && ssoStudentInfo && ssoStudentToken) {
    adminTokenState.clearAdminToken()
    studentTokenState.setStudentToken(ssoStudentInfo, ssoStudentStuNum || ssoStudentInfo.stuNum, ssoStudentToken)
  }

  if (ssoMode === 'admin' && ssoAdminProfile && (ssoAdminToken || ssoAdminProfile.token)) {
    studentTokenState.clearStudentToken()
    adminTokenState.setAdminToken({
      ...ssoAdminProfile,
      token: ssoAdminToken || ssoAdminProfile.token,
    })
  }

  if (ssoLoginUser) {
    localStorage.setItem('loginUser', JSON.stringify(ssoLoginUser))
  }

  const ssoKeys = [
    'ssoMode',
    'ssoStudentToken',
    'ssoStudentStuNum',
    'ssoDormId',
    'ssoStudentInfo',
    'ssoAdminToken',
    'ssoAdminProfile',
    'ssoLoginUser',
  ]
  const hasSso = ssoKeys.some((key) => key in nextQuery)
  if (hasSso) {
    ssoKeys.forEach((key) => {
      delete nextQuery[key]
    })
    return {
      path: to.path,
      query: nextQuery,
      hash: to.hash,
      replace: true,
    }
  }

  if (to.meta.guestOnly) {
    return resolveGuestRedirect(
      requestedRole,
      to.query.redirect,
      adminTokenState.isAdminLoggedIn,
      studentTokenState.isLoggedIn,
    )
  }

  if (to.meta.requiresAdminAuth && !adminTokenState.isAdminLoggedIn) {
    return redirectToLogin(to.fullPath, 'admin', '请先登录管理员账号')
  }

  if (to.meta.requiresStudentAuth && !studentTokenState.isLoggedIn) {
    return redirectToLogin(to.fullPath, 'student', '请先登录学生账号')
  }

  return true
})

export default router
