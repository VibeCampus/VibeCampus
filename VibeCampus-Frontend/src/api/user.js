import http from './index'
import { normalizeUser } from './normalize'

const userApi = {
  /**
   * 当前登录用户详情
   * 后端：GET /api/users/me
   */
  getCurrentUserDetail() {
    return http.get('/users/me').then(res => normalizeUser(res) || res)
  },

  /**
   * 指定用户详情
   * 后端：GET /api/users/{userId}
   * @param {string|number} userId
   */
  getUserDetail(userId) {
    return http.get(`/users/${userId}`).then(res => normalizeUser(res) || res)
  },

  /** 同 getCurrentUserDetail，保留旧名 */
  getMe() {
    return userApi.getCurrentUserDetail()
  },

  /**
   * 以下接口若后端尚未提供，会返回 404，可后续再对齐
   */
  updateProfile(data) {
    return http.put('/users/me', data)
  },

  uploadAvatar(formData) {
    return http.post('/users/me/avatar', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
  },

  changePassword(data) {
    return http.put('/users/me/password', data)
  },

  getMyPosts(params = {}) {
    return http.get('/users/me/posts', { params })
  },

  getMyComments(params = {}) {
    return http.get('/users/me/comments', { params })
  },

  getMyFavorites(params = {}) {
    return http.get('/users/me/favorites', { params })
  },

  getPublicProfile(userId) {
    return userApi.getUserDetail(userId)
  },
}

export default userApi
