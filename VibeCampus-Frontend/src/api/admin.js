import http from './index'

/**
 * 后台管理模块 API
 * 所有接口均需要管理员权限（后端通过 JWT role 验证）
 */
const adminApi = {
  // ── 管理员认证 ───────────────────────────────────────────

  /**
   * 管理员登录
   * POST /admin/auth/login
   * @param {{ account: string, password: string, captchaAnswer: string }} data
   * @returns {{ token: string, admin: AdminInfo }}
   */
  login(data) {
    return http.post('/admin/auth/login', data)
  },

  // ── 仪表盘 ───────────────────────────────────────────────

  /**
   * 获取仪表盘统计数据
   * GET /admin/dashboard/stats
   * @returns {{ totalUsers: number, newPosts: number, pendingReviews: number, totalComments: number, ... }}
   */
  getDashboardStats() {
    return http.get('/admin/dashboard/stats')
  },

  /**
   * 获取最近帖子列表（仪表盘用）
   * GET /admin/dashboard/recent-posts
   * @param {{ limit?: number }} params
   */
  getRecentPosts(params = { limit: 10 }) {
    return http.get('/admin/dashboard/recent-posts', { params })
  },

  // ── 内容管理 ─────────────────────────────────────────────

  /**
   * 获取帖子列表（含筛选）
   * GET /admin/posts
   * @param {{ userId?: string, category?: string, keyword?: string, status?: string, type?: string, anonymous?: boolean, page?: number, pageSize?: number }} params
   */
  getPosts(params = {}) {
    return http.get('/admin/posts', { params })
  },

  /**
   * 审核通过帖子
   * PUT /admin/posts/:id/approve
   */
  approvePost(id) {
    return http.put(`/admin/posts/${id}/approve`)
  },

  /**
   * 拒绝/下架帖子
   * PUT /admin/posts/:id/reject
   * @param {{ reason?: string }} data
   */
  rejectPost(id, data = {}) {
    return http.put(`/admin/posts/${id}/reject`, data)
  },

  /**
   * 批量删除帖子
   * DELETE /admin/posts
   * @param {{ ids: number[] }} data
   */
  deletePosts(ids) {
    return http.delete('/admin/posts', { data: { ids } })
  },

  // ── 分类管理 ─────────────────────────────────────────────

  /**
   * 获取分类树列表
   * GET /admin/categories
   * @returns {Category[]}
   */
  getCategories() {
    return http.get('/admin/categories')
  },

  /**
   * 新增分类
   * POST /admin/categories
   * @param {{ name: string, parentId?: number, sort?: number }} data
   */
  createCategory(data) {
    return http.post('/admin/categories', data)
  },

  /**
   * 更新分类
   * PUT /admin/categories/:id
   * @param {string|number} id
   * @param {{ name?: string, status?: 0|1, sort?: number }} data
   */
  updateCategory(id, data) {
    return http.put(`/admin/categories/${id}`, data)
  },

  /**
   * 删除分类
   * DELETE /admin/categories/:id
   */
  deleteCategory(id) {
    return http.delete(`/admin/categories/${id}`)
  },

  // ── 评论管理 ─────────────────────────────────────────────

  /**
   * 获取评论列表（含筛选）
   * GET /admin/comments
   * @param {{ keyword?: string, userId?: string, postId?: string, page?: number, pageSize?: number }} params
   */
  getComments(params = {}) {
    return http.get('/admin/comments', { params })
  },

  /**
   * 批量删除评论（级联删除回复）
   * DELETE /admin/comments
   * @param {{ ids: number[] }} data
   */
  deleteComments(ids) {
    return http.delete('/admin/comments', { data: { ids } })
  },

  // ── 用户管理 ─────────────────────────────────────────────

  /**
   * 获取用户列表
   * GET /admin/users
   * @param {{ keyword?: string, status?: 0|1, roleId?: number, page?: number, pageSize?: number }} params
   */
  getUsers(params = {}) {
    return http.get('/admin/users', { params })
  },

  /**
   * 更新用户状态（启用/禁用）
   * PUT /admin/users/:id/status
   * @param {string|number} id
   * @param {{ status: 0|1 }} data
   */
  updateUserStatus(id, data) {
    return http.put(`/admin/users/${id}/status`, data)
  },

  /**
   * 分配用户角色
   * PUT /admin/users/:id/role
   * @param {string|number} id
   * @param {{ roleId: number }} data
   */
  updateUserRole(id, data) {
    return http.put(`/admin/users/${id}/role`, data)
  },

  /**
   * 重置用户密码
   * PUT /admin/users/:id/reset-password
   * @returns {{ newPassword: string }}  后端返回临时密码
   */
  resetUserPassword(id) {
    return http.put(`/admin/users/${id}/reset-password`)
  },

  /**
   * 批量导入用户（Excel/CSV）
   * POST /admin/users/import
   * @param {FormData} formData
   */
  importUsers(formData) {
    return http.post('/admin/users/import', formData)
  },

  /**
   * 导出用户列表
   * GET /admin/users/export
   * @param {object} params  同 getUsers 的筛选参数
   * @returns {Blob}  下载文件
   */
  exportUsers(params = {}) {
    return http.get('/admin/users/export', { params, responseType: 'blob' })
  },

  // ── 角色权限管理 ─────────────────────────────────────────

  /**
   * 获取角色列表
   * GET /admin/roles
   */
  getRoles() {
    return http.get('/admin/roles')
  },

  /**
   * 创建角色
   * POST /admin/roles
   * @param {{ name: string, permissions: string[] }} data
   */
  createRole(data) {
    return http.post('/admin/roles', data)
  },

  /**
   * 更新角色
   * PUT /admin/roles/:id
   * @param {string|number} id
   * @param {{ name?: string, permissions?: string[] }} data
   */
  updateRole(id, data) {
    return http.put(`/admin/roles/${id}`, data)
  },

  /**
   * 删除角色
   * DELETE /admin/roles/:id
   */
  deleteRole(id) {
    return http.delete(`/admin/roles/${id}`)
  },

  // ── 敏感词管理 ───────────────────────────────────────────

  /**
   * 获取敏感词列表
   * GET /admin/sensitive-words
   * @param {{ keyword?: string, category?: string, strategy?: string, page?: number, pageSize?: number }} params
   */
  getSensitiveWords(params = {}) {
    return http.get('/admin/sensitive-words', { params })
  },

  /**
   * 新增敏感词
   * POST /admin/sensitive-words
   * @param {{ word: string, category: string, strategy: 'block'|'review'|'log' }} data
   */
  createSensitiveWord(data) {
    return http.post('/admin/sensitive-words', data)
  },

  /**
   * 更新敏感词
   * PUT /admin/sensitive-words/:id
   */
  updateSensitiveWord(id, data) {
    return http.put(`/admin/sensitive-words/${id}`, data)
  },

  /**
   * 删除敏感词
   * DELETE /admin/sensitive-words/:id
   */
  deleteSensitiveWord(id) {
    return http.delete(`/admin/sensitive-words/${id}`)
  },

  // ── 公告管理 ─────────────────────────────────────────────

  /**
   * 获取公告列表
   * GET /admin/announcements
   * @param {{ status?: string, page?: number, pageSize?: number }} params
   */
  getAnnouncements(params = {}) {
    return http.get('/admin/announcements', { params })
  },

  /**
   * 创建公告
   * POST /admin/announcements
   * @param {{ title: string, content: string, type: string, displayMethod: string[], status: string }} data
   */
  createAnnouncement(data) {
    return http.post('/admin/announcements', data)
  },

  /**
   * 更新公告
   * PUT /admin/announcements/:id
   */
  updateAnnouncement(id, data) {
    return http.put(`/admin/announcements/${id}`, data)
  },

  /**
   * 发布/撤回公告
   * PUT /admin/announcements/:id/publish
   * @param {string|number} id
   * @param {{ action: 'publish'|'withdraw' }} data
   */
  toggleAnnouncementStatus(id, data) {
    return http.put(`/admin/announcements/${id}/publish`, data)
  },

  /**
   * 删除公告
   * DELETE /admin/announcements/:id
   */
  deleteAnnouncement(id) {
    return http.delete(`/admin/announcements/${id}`)
  },

  // ── 日志管理 ─────────────────────────────────────────────

  /**
   * 获取操作日志
   * GET /admin/logs/operation
   * @param {{ keyword?: string, module?: string, type?: string, startTime?: string, endTime?: string, page?: number, pageSize?: number }} params
   */
  getOperationLogs(params = {}) {
    return http.get('/admin/logs/operation', { params })
  },

  /**
   * 获取登录日志
   * GET /admin/logs/login
   * @param {{ keyword?: string, status?: string, startTime?: string, endTime?: string, page?: number, pageSize?: number }} params
   */
  getLoginLogs(params = {}) {
    return http.get('/admin/logs/login', { params })
  },
}

export default adminApi
