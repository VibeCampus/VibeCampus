import http from './index'
import { normalizeUser, parseListPayload } from './normalize'

/**
 * 关注关系 API。后端路径：
 *   POST   /api/users/{id}/follow
 *   DELETE /api/users/{id}/follow
 *   GET    /api/users/{id}/following
 *   GET    /api/users/{id}/followers
 *
 * 后端 FollowPageResponse: { list: UserDetailResponse[], total, page, pageSize }
 */
function normalizeFollowPage(raw) {
  const list = parseListPayload(raw).map(u => normalizeUser(u) || u).filter(Boolean)
  return {
    list,
    total: raw?.total ?? list.length,
    page: raw?.page,
    pageSize: raw?.pageSize,
    raw,
  }
}

const followApi = {
  follow(userId) {
    return http.post(`/users/${userId}/follow`)
  },

  unfollow(userId) {
    return http.delete(`/users/${userId}/follow`)
  },

  getFollowingList(userId, params = {}) {
    return http.get(`/users/${userId}/following`, { params }).then(normalizeFollowPage)
  },

  getFollowerList(userId, params = {}) {
    return http.get(`/users/${userId}/followers`, { params }).then(normalizeFollowPage)
  },
}

export default followApi
