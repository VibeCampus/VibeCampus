import http from './index'
import { normalizeUser, normalizePost, normalizeComment, parseListPayload } from './normalize'

/**
 * 将分页响应中的 list 用指定 normalizer 处理后回填，保留 total/page/pageSize。
 */
function normalizePage(raw, normalizer) {
  const list = parseListPayload(raw).map(item => normalizer(item) || item).filter(Boolean)
  return {
    list,
    total: raw?.total ?? list.length,
    page: raw?.page,
    pageSize: raw?.pageSize,
    raw,
  }
}

const userApi = {
  /**
   * 当前登录用户详情
   * 后端：GET /api/user/me
   */
  getCurrentUserDetail() {
    return http.get('/user/me').then(res => normalizeUser(res) || res)
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

  updateProfile(data) {
    return http.put('/user/me', data).then(res => normalizeUser(res) || res)
  },

  uploadAvatar(formData) {
    return http.post('/user/me/avatar', formData)
  },

  changePassword(data) {
    return http.put('/user/me/password', data)
  },

  /**
   * GET /api/user/me/posts —— 当前用户发布的帖子（分页）
   */
  getMyPosts(params = {}) {
    return http.get('/user/me/posts', { params }).then(raw => normalizePage(raw, normalizePost))
  },

  /**
   * GET /api/user/me/comments —— 当前用户发表的评论（含 postTitle，分页）
   */
  getMyComments(params = {}) {
    return http.get('/user/me/comments', { params }).then(raw => normalizePage(raw, normalizeComment))
  },

  /**
   * GET /api/user/me/favorites —— 当前用户收藏的帖子（分页）
   */
  getMyFavorites(params = {}) {
    return http.get('/user/me/favorites', { params }).then(raw => normalizePage(raw, normalizePost))
  },

  /**
   * GET /api/users/{id}/posts —— 指定用户发布的帖子（匿名帖会自动过滤）
   */
  getUserPosts(userId, params = {}) {
    return http.get(`/users/${userId}/posts`, { params }).then(raw => normalizePage(raw, normalizePost))
  },

  /**
   * GET /api/users/{id}/comments —— 指定用户的评论
   */
  getUserComments(userId, params = {}) {
    return http.get(`/users/${userId}/comments`, { params }).then(raw => normalizePage(raw, normalizeComment))
  },

  /**
   * DELETE /api/user/me —— 注销当前账号（软删除 + 吊销 token）
   */
  deleteAccount() {
    return http.delete('/user/me')
  },

  getPublicProfile(userId) {
    return userApi.getUserDetail(userId)
  },
}

export default userApi
