import { useEffect, useState } from 'react'
import { useNavigate, useParams, useSearchParams } from 'react-router-dom'
import { renderView, publishBook } from './api.js'
import UniversalRenderer from '@yuku123/z-frontend-render'

/**
 * 视图渲染页 (消费 /api/lc/report/render/view/{id})。
 * 带 ?bookId= 时显示"整体发布"按钮: 发布成功展示分享链接 (观看侧 /share/)。
 */
export default function ViewPage() {
  const { viewId } = useParams()
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const bookId = searchParams.get('bookId')
  const [data, setData] = useState(null)
  const [error, setError] = useState('')
  const [shareLink, setShareLink] = useState('')

  useEffect(() => {
    renderView(viewId)
      .then(setData)
      .catch((e) => setError(String(e.message || e)))
  }, [viewId])

  const onPublish = async () => {
    setError('')
    try {
      const pub = await publishBook(bookId)
      const link = `${window.location.origin}/z-lc/report/share/${pub.publishId}?token=${pub.shareToken}`
      setShareLink(link)
    } catch (e) {
      setError(String(e.message || e))
    }
  }

  return (
    <div>
      <div style={{ padding: '12px 16px', borderBottom: '1px solid #f0f0f0' }}>
        <a
          href="/z-lc/report"
          onClick={(e) => { e.preventDefault(); navigate('/z-lc/report') }}
        >
          ← 首页
        </a>
        {bookId ? (
          <button
            type="button"
            onClick={onPublish}
            style={{ marginLeft: 16, padding: '4px 14px', cursor: 'pointer' }}
          >
            整体发布
          </button>
        ) : null}
        {shareLink ? (
          <a href={shareLink} style={{ marginLeft: 16 }} target="_blank" rel="noreferrer">
            分享链接 (打开观看页)
          </a>
        ) : null}
      </div>
      {error ? (
        <div style={{ color: '#cf1322', background: '#fff1f0', padding: 12, margin: 16, borderRadius: 8 }}>
          {error}
        </div>
      ) : (
        <UniversalRenderer renderData={data} />
      )}
    </div>
  )
}
