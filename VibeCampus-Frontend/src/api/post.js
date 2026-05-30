import http from './index'
import { normalizePost, parseListPayload } from './normalize'

/**
 * 帖子模块 API
 *
 * 后端只暴露了 GET /api/posts、GET /api/posts/{id}、POST /api/posts（multipart）。
 * 帖子级的点赞/收藏接口尚未在 PostController 中暴露（mapper 已具备能力），
 * 因此这里暂时不提供 toggleLike / toggleFavorite 调用，UI 侧采用乐观更新即可。
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
}

export default postApi
