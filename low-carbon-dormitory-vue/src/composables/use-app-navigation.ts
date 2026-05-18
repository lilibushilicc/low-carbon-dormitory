import { useRoute, useRouter } from 'vue-router'
import { useRouteTransitionState } from '@/router/route-transition-state'
import { logoutAndRedirect } from '@/utils/auth-session'

export function useAppNavigation() {
  const route = useRoute()
  const router = useRouter()
  const { isRouteNavigating, pendingRoutePath } = useRouteTransitionState()

  function goTo(path: string) {
    if (route.path === path) {
      return
    }

    router.push(path)
  }

  function isActive(path: string) {
    return route.path === path || route.path.startsWith(`${path}/`)
  }

  function isPending(path: string) {
    return isRouteNavigating.value && pendingRoutePath.value === path
  }

  async function logout() {
    await logoutAndRedirect(router)
  }

  return {
    route,
    router,
    goTo,
    isActive,
    isPending,
    logout,
  }
}
