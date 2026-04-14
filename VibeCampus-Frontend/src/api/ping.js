import http from './index'

/**
 * 联调/连通性检测
 */
const pingApi = {
  /**
   * GET /ping
   * @returns {{ app: string, status: string, timestamp: string }}
   */
  ping() {
    return http.get('/ping')
  },
}

export default pingApi

