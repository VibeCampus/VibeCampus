import http from './index'

/**
 * 评论模块 API
 *
 * 注意：后端路径不一致 —— GET 用单数 `/post/{postId}/comments`，
 * 而 POST 用复数 `/posts/{postId}/comments`。这里如实保留。
 */
const commentApi = {
  /**
   * 获取帖子的评论列表（含嵌套回复）
   * GET /api/post/:postId/comments
   * @param {string|number} postId
   * @param {{ page?: number, pageSize?: number }} params
   * @returns {{ list: CommentResponse[], total: number, page: number, pageSize: number }}
   */
  getList(postId, params = {}) {
    return http.get(`/post/${postId}/comments`, { params })
  },

  /**
   * 发布一级评论
   * POST /api/posts/:postId/comments
   * @param {string|number} postId
   * @param {{ content: string }} data
   * @returns {CommentResponse}
   */
  create(postId, data) {
    return http.post(`/posts/${postId}/comments`, data)
  },

  /**
   * 回复某条评论（二级回复）
   * POST /api/comments/:commentId/replies
   * @param {string|number} commentId  被回复的一级评论 ID
   * @param {{ content: string, replyToUserId?: number }} data
   * @returns {CommentReplyResponse}
   */
  reply(commentId, data) {
    return http.post(`/comments/${commentId}/replies`, data)
  },

  /**
   * 点赞 / 取消点赞评论（幂等切换）
   * POST /api/comments/:commentId/like
   * @returns {{ liked: boolean, likeCount: number }}
   */
  toggleLike(commentId) {
    return http.post(`/comments/${commentId}/like`)
  },

  /**
   * 删除自己的评论（软删除）
   * DELETE /api/comments/:commentId
   */
  remove(commentId) {
    return http.delete(`/comments/${commentId}`)
  },
}

export default commentApi
