// 后端 API 薄封装: 错误统一抛出 {status, message}, 页面层捕获展示。
// 约定: 后端异常响应 {status, error, message} (ApiErrorAdvice), 业务成功直接返回 JSON。

async function request(url, options = {}) {
  const resp = await fetch(url, {
    headers: { 'Content-Type': 'application/json' },
    ...options,
  })
  const text = await resp.text()
  const body = text ? JSON.parse(text) : null
  if (!resp.ok) {
    const msg = (body && (body.message || body.error)) || `HTTP ${resp.status}`
    throw new Error(msg)
  }
  return body
}

/** 渲染 API */

/** 整页渲染: ViewSchema → {viewId, title, version, layout, options} */
export function renderView(viewId) {
  return request(`/api/lc/report/render/view/${encodeURIComponent(viewId)}`)
}

/** 单图表渲染 (拖拽编辑器预览用) */
export function renderWidget(widgetSpec) {
  return request('/api/lc/report/render/widget', {
    method: 'POST',
    body: JSON.stringify(widgetSpec),
  })
}

/** 模板 API */

/** 内置模板清单 */
export function listTemplates() {
  return request('/api/lc/report/templates')
}

/** 应用模板 → {bookId, pageId, viewId, datasetId, sourceId} */
export function applyTemplate(tplId) {
  return request(`/api/lc/report/templates/${encodeURIComponent(tplId)}/apply`, { method: 'POST' })
}

/** 书 API */

/** 书节点树 */
export function bookTree(bookId) {
  return request(`/api/lc/report/book/${encodeURIComponent(bookId)}/tree`)
}

/** 整体发布 → PublicationSnapshot (含 publishId/shareToken) */
export function publishBook(bookId) {
  return request(`/api/lc/report/book/${encodeURIComponent(bookId)}/publish`, { method: 'POST' })
}

/** 读取发布快照 (观看侧, token 必带) */
export function getPublication(publishId, token) {
  return request(
    `/api/lc/report/book/publication/${encodeURIComponent(publishId)}?token=${encodeURIComponent(token)}`,
  )
}

/** AI API (M3): 一句话生成。请求体字段为 {requirement, sourceId} / {requirement} */

/** NL → 数据集定义 */
export function aiDataset(requirement, sourceId) {
  return request('/api/lc/report/ai/dataset', {
    method: 'POST',
    body: JSON.stringify({ requirement, sourceId }),
  })
}

/** NL → 视图页 (复用已有数据集) */
export function aiView(requirement) {
  return request('/api/lc/report/ai/view', {
    method: 'POST',
    body: JSON.stringify({ requirement }),
  })
}
