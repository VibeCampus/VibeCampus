import http from './index'
import { normalizePost, parseListPayload } from './normalize'

/**
 * 帖子模块 API
 *
 * 后端 PostController 暴露：
 *  - GET    /api/posts
 *  - GET    /api/posts/{id}
 *  - POST   /api/posts (multipart)
 *  - POST   /api/posts/{id}/like      切换点赞
 *  - POST   /api/posts/{id}/favorite  切换收藏
 *  - DELETE /api/posts/{id}           软删除（仅作者本人）
 */
const postApi = {
  /**
   * GET /api/posts
   * Params: { category?, page?, pageSize? }
   * Response: { list: PostResponse[], total, page, pageSize }
   */
  getList(params = {}) {
    return http.get('/posts', { params }).then(raw => {
      const list = parseListPayload(raw).map(p => normalizePost(p) || p)
      return {
        list,
        total: raw?.total ?? list.length,
        page: raw?.page,
        pageSize: raw?.pageSize,
        raw,
      }
    })
  },

  /**
   * GET /api/posts/{id}
   * Response: PostResponse
   */
  getDetail(id) {
    return http.get(`/posts/${id}`).then(res => normalizePost(res) || res)
  },

  /**
   * POST /api/posts (multipart/form-data)
   * Parts: category, content, anonymous, images[]?, video?
   *
   * 入参可以是已构造好的 FormData，或者一个普通对象 { category, content, anonymous, images?, video? }。
   */
  create(body) {
    if (body instanceof FormData) {
      return http.post('/posts', body).then(res => normalizePost(res) || res)
    }
    const formData = new FormData()
    formData.append('category', body.category)
    formData.append('content', body.content)
    formData.append('anonymous', String(body.anonymous ?? false))
    if (Array.isArray(body.images)) {
      body.images.forEach(file => {
        if (file) formData.append('images', file)
      })
    }
    if (body.video) {
      formData.append('video', body.video)
    }
    return http.post('/posts', formData).then(res => normalizePost(res) || res)
  },

  /**
   * POST /api/posts/{id}/like
   * Response: { liked, likeCount }
   */
  toggleLike(id) {
    return http.post(`/posts/${id}/like`)
  },

  /**
   * POST /api/posts/{id}/favorite
   * Response: { favorited, favoriteCount }
   */
  toggleFavorite(id) {
    return http.post(`/posts/${id}/favorite`)
  },

  /**
   * DELETE /api/posts/{id}
   */
  remove(id) {
    return http.delete(`/posts/${id}`)
  },
}

export default postApi
