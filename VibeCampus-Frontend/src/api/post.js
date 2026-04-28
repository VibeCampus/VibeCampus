import http from './index'
import { normalizePost, parseListPayload } from './normalize'

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
    return http.get(`/posts/${id}`)
  },

  /**
   * POST /api/posts (multipart/form-data)
   * Parts: category, content, anonymous, images[]?, video?
   */
  create(body) {
    if (body instanceof FormData) {
      return http.post('/posts', body, {
        headers: { 'Content-Type': 'multipart/form-data' },
      })
    }
    const formData = new FormData()
    formData.append('category', body.category)
    formData.append('content', body.content)
    formData.append('anonymous', String(body.anonymous ?? false))
    return http.post('/posts', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
  },

  remove(id) {
    return http.delete(`/posts/${id}`)
  },

  toggleLike(id) {
    return http.post(`/posts/${id}/like`)
  },

  toggleFavorite(id) {
    return http.post(`/posts/${id}/favorite`)
  },

  search(params) {
    return http.get('/posts/search', { params })
  },

  getHotList(params = { limit: 10 }) {
    return http.get('/posts/hot', { params })
  },
}

export default postApi
