import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { listTemplates, applyTemplate } from './api.js'

/**
 * 首页: 内置模板画廊 (M2)。apply 成功 → 跳转视图页。
 * AI 一句话生成入口 (M3) 挂在 AI 生成页 (未配置 LLM 时后端返回配置指引错误, 前端透出)。
 */
export default function Home() {
  const [templates, setTemplates] = useState([])
  const [error, setError] = useState('')
  const [busy, setBusy] = useState('')
  const navigate = useNavigate()

  useEffect(() => {
    listTemplates()
      .then(setTemplates)
      .catch((e) => setError(String(e.message || e)))
  }, [])

  const onApply = async (tplId) => {
    setBusy(tplId)
    setError('')
    try {
      const r = await applyTemplate(tplId)
      navigate(`/z-lc/report/view/${r.viewId}?bookId=${encodeURIComponent(r.bookId)}`)
    } catch (e) {
      setError(String(e.message || e))
    } finally {
      setBusy('')
    }
  }

  return (
    <div style={{ maxWidth: 960, margin: '0 auto', padding: 24 }}>
      <h1 style={{ fontSize: 22 }}>z-lc 报表平台</h1>
      <p style={{ color: '#666' }}>
        从模板创建示例报表书, 或用 AI 一句话生成 (需后端配置 z.report.llm.*)。
      </p>
      {error ? (
        <div style={{ color: '#cf1322', background: '#fff1f0', padding: 12, borderRadius: 8 }}>
          {error}
        </div>
      ) : null}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(280px, 1fr))', gap: 16, marginTop: 16 }}>
        {templates.map((t) => (
          <div
            key={t.id}
            style={{ border: '1px solid #f0f0f0', borderRadius: 8, padding: 16, background: '#fff' }}
          >
            <div style={{ fontWeight: 700, fontSize: 16 }}>{t.title}</div>
            <div style={{ color: '#666', fontSize: 13, margin: '8px 0 16px' }}>{t.description}</div>
            <button
              type="button"
              disabled={busy === t.id}
              onClick={() => onApply(t.id)}
              style={{
                padding: '6px 16px',
                borderRadius: 6,
                border: '1px solid #1677ff',
                background: '#1677ff',
                color: '#fff',
                cursor: 'pointer',
              }}
            >
              {busy === t.id ? '创建中...' : '使用模板创建'}
            </button>
          </div>
        ))}
        {templates.length === 0 && !error ? <div style={{ color: '#999' }}>加载中...</div> : null}
      </div>
      <div style={{ marginTop: 24 }}>
        <a
          href="/z-lc/report/ai"
          onClick={(e) => { e.preventDefault(); navigate('/z-lc/report/ai') }}
          style={{ color: '#1677ff' }}
        >
          AI 一句话生成 →
        </a>
      </div>
    </div>
  )
}
