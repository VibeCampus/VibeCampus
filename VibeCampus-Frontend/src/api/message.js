const NOT_IMPLEMENTED_NOTE = '消息中心功能尚未上线'
let warned = false

function emptyPage() {
  return { list: [], total: 0, unreadCount: 0 }
}

function warnOnce() {
  if (warned) return
  warned = true
  if (typeof window !== 'undefined') {
    import('@/composables/useToast').then(({ toast }) => toast.info(NOT_IMPLEMENTED_NOTE))
  }
}

/**
 * 消息模块 API
 *
 * 注意：后端目前未提供 /notifications、/messages/* 端点，所有方法暂时返回空数据。
 * 等后端补齐后，把 fallback 替换成真正的 http 请求即可。
 */
const messageApi = {
  getNotifications() {
    warnOnce()
    return Promise.resolve(emptyPage())
  },

  markNotificationsRead() {
    warnOnce()
    return Promise.resolve()
  },

  getChatList() {
    warnOnce()
    return Promise.resolve({ list: [] })
  },

  getChatMessages() {
    warnOnce()
    return Promise.resolve({ list: [], total: 0 })
  },

  sendMessage() {
    warnOnce()
    return Promise.reject(new Error(NOT_IMPLEMENTED_NOTE))
  },

  markChatRead() {
    warnOnce()
    return Promise.resolve()
  },

  getSystemMessages() {
    warnOnce()
    return Promise.resolve(emptyPage())
  },

  markSystemRead() {
    warnOnce()
    return Promise.resolve()
  },

  getUnreadCount() {
    return Promise.resolve({ total: 0, notification: 0, chat: 0, system: 0 })
  },
}

export default messageApi
