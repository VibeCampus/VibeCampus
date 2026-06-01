import { reactive } from 'vue'

/**
 * 全局轻量 toast：无第三方依赖。
 *
 * 用法：
 *   import { toast } from '@/composables/useToast'
 *   toast.error('网络异常')
 *   toast.success('保存成功')
 */
const state = reactive({
  list: [],
})

let nextId = 1

const DEFAULT_DURATION = {
  error: 4000,
  success: 2400,
  info: 2800,
  warning: 3200,
}

function push(type, message, duration) {
  if (!message) return
  const id = nextId++
  const item = { id, type, message: String(message) }
  state.list.push(item)
  const ttl = duration ?? DEFAULT_DURATION[type] ?? 2800
  setTimeout(() => dismiss(id), ttl)
  return id
}

function dismiss(id) {
  const index = state.list.findIndex(item => item.id === id)
  if (index >= 0) state.list.splice(index, 1)
}

export const toast = {
  error: (msg, ms) => push('error', msg, ms),
  success: (msg, ms) => push('success', msg, ms),
  info: (msg, ms) => push('info', msg, ms),
  warning: (msg, ms) => push('warning', msg, ms),
  dismiss,
}

export function useToast() {
  return { toast, state }
}

export { state as toastState }
