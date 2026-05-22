import axios from 'axios'
import { unwrapBody } from './unwrap'

/**
 * axios 实例
 * baseURL 从环境变量读取，本地开发可在 .env.development 中设置：
 *   VITE_API_BASE_URL=/api  （建议配合 vite 代理，避免跨域）
 */
const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 15000,
  headers: { 'Content-Type': 'application/json' },
})

// ── 请求拦截器：自动带上 token ──────────────────────────────
http.interceptors.request.use(
  config => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  error => Promise.reject(error),
)

// ── 响应拦截器：统一解包 { code, data } 与错误处理 ───────────
http.interceptors.response.use(
  response => unwrapBody(response.data),
  error => {
    const status = error.response?.status
    const data = error.response?.data
    const msg = data?.message || data?.msg || error.message || '请求失败，请稍后重试'

    if (status === 401) {
      localStorage.removeItem('token')
      localStorage.removeItem('userInfo')
      window.location.href = '/userlogin'
      return Promise.reject(error)
    }

    if (status === 403) {
      console.warn('权限不足：', msg)
    }

    if (status === 404) {
      console.warn('资源不存在：', error.config?.url)
    }

    return Promise.reject(new Error(msg))
  },
)

export { http }
export { default as authApi } from './auth'
export { default as postApi } from './post'
export { default as commentApi } from './comment'
export { default as userApi } from './user'
export { default as messageApi } from './message'
export { default as adminApi } from './admin'
export { default as pingApi } from './ping'

export default http
