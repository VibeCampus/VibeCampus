import http from './index'
import { pickLoginData } from './normalize'
import userApi from './user'

const authApi = {
  /**
   * POST /api/auth/login
   * Body: { account, password, captcha, captchaId }
   * Response: { token, user: { id, username, phone } }
   */
  login(data) {
    const body = {
      account: data.account ?? data.username,
      password: data.password,
      captcha: data.captcha,
      captchaId: data.captchaId,
    }
    return http.post('/auth/login', body).then(async res => {
      const picked = pickLoginData(res)
      if (!picked.token) {
        return Promise.reject(new Error('登录响应缺少 token'))
      }
      if (picked.user) {
        return picked
      }
      localStorage.setItem('token', picked.token)
      try {
        const user = await userApi.getCurrentUserDetail()
        if (user) {
          return { ...picked, user }
        }
        return Promise.reject(new Error('无法获取当前用户信息'))
      } catch (e) {
        localStorage.removeItem('token')
        throw e
      }
    })
  },

  /**
   * POST /api/auth/register
   * Body: { username, password, captcha, gender?, phone?, email?, nickname? }
   * Response: { token, user: { id, username, phone } }
   */
  register(data) {
    return http
      .post('/auth/register', {
        username: data.username,
        password: data.password,
        captcha: data.captcha,
        captchaId: data.captchaId,
        phone: data.phone,
        gender: data.gender,
        email: data.email,
        nickname: data.nickname,
      })
      .then(res => {
        if (res && (res.token || res.accessToken || res.access_token)) {
          return pickLoginData(res)
        }
        return res
      })
  },

  /**
   * GET /api/auth/captcha
   * Response: { captchaId, image }  (image 为 base64)
   */
  getCaptcha() {
    return http.get('/auth/captcha')
  },

  sendSms(data) {
    return http.post('/auth/sms', data)
  },

  verifySmsCode(data) {
    return http.post('/auth/sms/verify', data)
  },

  resetPassword(data) {
    return http.post('/auth/reset-password', data)
  },

  /**
   * POST /api/auth/logout
   */
  logout() {
    return http.post('/auth/logout')
  },
}

export default authApi
