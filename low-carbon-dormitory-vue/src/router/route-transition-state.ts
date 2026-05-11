import { computed, ref } from 'vue'

const INITIAL_PROGRESS = 0.14
const PROGRESS_MAX = 0.9
const ENTER_DELAY_MS = 24
const FINISH_DELAY_MS = 220

export const isRouteNavigating = ref(false)
export const routeProgress = ref(0)
export const pendingRoutePath = ref('')

let progressTimer: ReturnType<typeof window.setInterval> | null = null
let finishTimer: ReturnType<typeof window.setTimeout> | null = null
let kickTimer: ReturnType<typeof window.setTimeout> | null = null

function clearRouteTimers() {
  if (progressTimer !== null) {
    window.clearInterval(progressTimer)
    progressTimer = null
  }

  if (finishTimer !== null) {
    window.clearTimeout(finishTimer)
    finishTimer = null
  }

  if (kickTimer !== null) {
    window.clearTimeout(kickTimer)
    kickTimer = null
  }
}

function tickProgress() {
  progressTimer = window.setInterval(() => {
    routeProgress.value = Math.min(PROGRESS_MAX, routeProgress.value + (1 - routeProgress.value) * 0.08)

    if (routeProgress.value >= PROGRESS_MAX) {
      clearRouteTimers()
    }
  }, 90)
}

export function beginRouteTransition(targetPath: string) {
  clearRouteTimers()
  pendingRoutePath.value = targetPath
  isRouteNavigating.value = true
  routeProgress.value = INITIAL_PROGRESS

  kickTimer = window.setTimeout(() => {
    routeProgress.value = 0.68
    tickProgress()
  }, ENTER_DELAY_MS)
}

export function finishRouteTransition() {
  clearRouteTimers()
  routeProgress.value = 1

  finishTimer = window.setTimeout(() => {
    isRouteNavigating.value = false
    routeProgress.value = 0
    pendingRoutePath.value = ''
    finishTimer = null
  }, FINISH_DELAY_MS)
}

export function cancelRouteTransition() {
  clearRouteTimers()
  isRouteNavigating.value = false
  routeProgress.value = 0
  pendingRoutePath.value = ''
}

export function useRouteTransitionState() {
  const progressScale = computed(() => `scaleX(${routeProgress.value})`)
  return {
    isRouteNavigating,
    routeProgress,
    progressScale,
    pendingRoutePath,
  }
}
