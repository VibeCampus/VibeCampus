/**
 * 解包后端响应体。
 *
 * 当前后端 Controller 直接返回业务对象（如 LoginResponse、PostPageResponse），
 * 没有 { code, message, data } 统一包装，因此大多数时候直接透传。
 *
 * 若日后后端统一包装为 { code, data }，下面的逻辑仍然兼容。
 */
export function unwrapBody(body) {
  if (body == null || typeof body !== 'object' || body instanceof ArrayBuffer) {
    return body
  }
  if (!('code' in body)) {
    return body
  }
  const c = body.code
  const ok = c === 0 || c === 200 || c === '0' || c === '200'
  if (ok) {
    return 'data' in body ? body.data : body
  }
  const msg = body.message || body.msg || '请求失败'
  return Promise.reject(new Error(msg))
}
