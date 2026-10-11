import { useEffect, useState } from 'react'
import { useParams, useSearchParams } from 'react-router-dom'
import { getPublication, renderView } from './api.js'
import UniversalRenderer from '@yuku123/z-frontend-render'

/**
 * 观看页 (匿名分享入口): /share/{publishId}?token=xxx
 * 读快照 → 找到第一个 PAGE 节点的 viewId → 渲染该视图。
 * 快照与编辑态隔离: 这里看到的是发布时刻的树 (整书发布语义)。
 */
export default function SharePage() {
  const { publishId } = useParams()
  const [searchParams] = useSearchParams()
  const token = searchParams.get('token')
  const [data, setData] = useState(null)
  const [error, setError] = useState('')

  useEffect(() => {
    let cancelled = false
    async function load() {
      try {
        const pub = await getPublication(publishId, token)
        const pages = (pub.nodes || []).filter((n) => n.nodeType === 'PAGE' && n.viewId)
        if (pages.length === 0) {
          throw new Error('该发布快照中没有页面节点')
        }
        const rendered = await renderView(pages[0].viewId)
        if (!cancelled) setData(rendered)
      } catch (e) {
        if (!cancelled) setError(String(e.message || e))
      }
    }
    load()
    return () => {
      cancelled = true
    }
  }, [publishId, token])

  if (error) {
    return (
      <div style={{ color: '#cf1322', background: '#fff1f0', padding: 12, margin: 16, borderRadius: 8 }}>
        {error}
      </div>
    )
  }
  return <UniversalRenderer renderData={data} />
}
