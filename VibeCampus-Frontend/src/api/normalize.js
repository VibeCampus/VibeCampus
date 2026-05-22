/**
 * 将后端 DTO 字段归一化为前端 UI 使用的结构。
 *
 * 后端 UserDetailResponse: { id, username, nickname, email, phone, gender, status, isCurrentUser }
 * 后端 UserInfo (login): { id, username, phone }
 * 后端 PostAuthorResponse: { id, username, avatar }
 */
export function normalizeUser(u) {
  if (!u || typeof u !== 'object') return null
  return {
    id: u.id,
    username: u.username ?? u.nickname ?? '',
    nickname: u.nickname ?? u.username ?? '',
    avatar: u.avatar ?? u.avatarUrl ?? u.avatar_url ?? '',
    phone: u.phone ?? '',
    email: u.email ?? '',
    gender: normalizeGender(u.gender),
    bio: u.bio ?? '',
    major: u.major ?? '',
    status: u.status,
    isCurrentUser: u.isCurrentUser,
    joinedAt: u.joinedAt ?? u.createdAt ?? u.createTime,
  }
}

function normalizeGender(g) {
  if (g == null) return '保密'
  if (typeof g === 'string') return g
  const map = { 0: '保密', 1: '男', 2: '女', 3: '其他' }
  return map[g] ?? '保密'
}

/**
 * 后端 PostResponse: { id, category, content, anonymous, authorId, author, images, time, likes, comments }
 * 后端 PostAuthorResponse: { id, username, avatar }
 */
export function normalizePost(p) {
  if (!p || typeof p !== 'object') return null
  const authorRaw = p.author ?? p.user ?? p.userInfo
  const author = authorRaw ? normalizeUser(authorRaw) : null
  const authorId = p.authorId ?? p.userId ?? author?.id ?? null
  return {
    id: p.id,
    authorId: authorId != null ? Number(authorId) : null,
    author,
    category: p.category ?? p.categorySlug ?? '',
    anonymous: p.anonymous ?? false,
    content: p.content ?? '',
    time: p.time ?? p.createdAt ?? p.createTime ?? '刚刚',
    likes: p.likes ?? p.likeCount ?? 0,
    comments: p.comments ?? p.commentCount ?? 0,
    favorites: p.favorites ?? p.favoriteCount ?? 0,
    images: p.images ?? [],
  }
}

export function pickLoginData(data) {
  if (!data || typeof data !== 'object') {
    return { token: '', user: null }
  }
  const token = data.token ?? data.accessToken ?? data.access_token ?? ''
  const rawUser = data.user ?? data.userInfo
  if (rawUser) {
    return { token, user: normalizeUser(rawUser) }
  }
  if (data.id != null && (data.username != null || data.phone != null)) {
    return { token, user: normalizeUser(data) }
  }
  return { token, user: null }
}

export function parseListPayload(raw) {
  if (raw == null) return []
  if (Array.isArray(raw)) return raw
  if (raw.list) return raw.list
  if (raw.records) return raw.records
  if (raw.content) return raw.content
  if (raw.rows) return raw.rows
  return []
}
