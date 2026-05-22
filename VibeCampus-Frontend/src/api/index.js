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

const PUBLIC_AUTH_PATHS = [
  '/auth/captcha',
  '/auth/login',
  '/auth/register',
  '/auth/sms',
  '/auth/sms/verify',
  '/auth/reset-password',
  '/admin/auth/login',
]

function getRequestPath(config = {}) {
  const rawUrl = config.url || ''
  const origin = typeof window === 'undefined' ? 'http://localhost' : window.location.origin

  try {
    const parsed = new URL(rawUrl, origin)
    return parsed.pathname.replace(/^\/api(?=\/)/, '')
  } catch {
    return rawUrl.split('?')[0].replace(/^\/api(?=\/)/, '')
  }
}

function isPublicAuthRequest(config) {
  const path = getRequestPath(config)
  return PUBLIC_AUTH_PATHS.some(publicPath => path === publicPath || path.startsWith(`${publicPath}/`))
}

function removeAuthorizationHeader(headers) {
  if (!headers) return
  if (typeof headers.delete === 'function') {
    headers.delete('Authorization')
    headers.delete('authorization')
    return
  }
  delete headers.Authorization
  delete headers.authorization
}

// ── 请求拦截器：自动带上 token ──────────────────────────────
http.interceptors.request.use(
  config => {
    if (isPublicAuthRequest(config)) {
      removeAuthorizationHeader(config.headers)
      return config
    }

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
      if (typeof localStorage !== 'undefined') {
        localStorage.removeItem('token')
        localStorage.removeItem('userInfo')
      }

      if (!isPublicAuthRequest(error.config) && typeof window !== 'undefined') {
        const requestPath = getRequestPath(error.config)
        const loginPath = requestPath.startsWith('/admin/') ? '/admin/login' : '/userlogin'
        if (window.location.pathname !== loginPath) {
          window.location.href = loginPath
        }
      }
      return Promise.reject(new Error(msg))
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
