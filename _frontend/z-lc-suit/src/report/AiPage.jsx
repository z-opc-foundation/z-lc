import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { aiDataset, aiView } from './api.js'

/**
 * AI 生成页 (M3): 一句话 → 数据集 → 视图。
 * 两步走: 先选数据源生成数据集, 再基于全部已注册数据集生成视图页。
 * 后端 LLM 未配置时, 返回配置指引错误 (前端原样透出, 见 ZReportBeanConfig 缺省实现)。
 */
export default function AiPage() {
  const [sources, setSources] = useState([])
  const [sourceId, setSourceId] = useState('')
  const [requirement, setRequirement] = useState('')
  const [log, setLog] = useState([])
  const [busy, setBusy] = useState(false)
  const navigate = useNavigate()

  useEffect(() => {
    // 数据源清单: 后端 /api/lc/report/datasource 端点 (DataSourceController); 失败不阻塞手输 sourceId
    fetch('/api/lc/report/datasource')
      .then((r) => (r.ok ? r.json() : []))
      .then((list) => {
        setSources(list)
        if (list.length > 0) setSourceId(list[0].id)
      })
      .catch(() => {})
  }, [])

  const pushLog = (line) => setLog((prev) => [...prev, line])

  const run = async () => {
    setBusy(true)
    setLog([])
    try {
      if (!sourceId) throw new Error('请选择数据源 (或先在数据源页注册)')
      pushLog(`生成数据集: ${requirement}`)
      const ds = await aiDataset(requirement, sourceId)
      pushLog(`数据集已生成: ${ds.id} (主表 ${ds.baseTable})`)
      const view = await aiView(requirement)
      pushLog(`视图已生成: ${view.id}, 跳转渲染页...`)
      navigate(`/z-lc/report/view/${view.id}`)
    } catch (e) {
      pushLog(`失败: ${e.message || e}`)
    } finally {
      setBusy(false)
    }
  }

  return (
    <div style={{ maxWidth: 720, margin: '0 auto', padding: 24 }}>
      <h1 style={{ fontSize: 20 }}>AI 一句话生成报表</h1>
      <div style={{ margin: '12px 0' }}>
        <select
          value={sourceId}
          onChange={(e) => setSourceId(e.target.value)}
          style={{ padding: 6, marginRight: 8 }}
        >
          {sources.map((s) => (
            <option key={s.id} value={s.id}>
              {s.id} ({s.type})
            </option>
          ))}
          {sources.length === 0 ? <option value="">(无已注册数据源)</option> : null}
        </select>
      </div>
      <textarea
        value={requirement}
        onChange={(e) => setRequirement(e.target.value)}
        placeholder="例: 展示各城市销售额柱状图, 加一个销售总额 KPI"
        rows={3}
        style={{ width: '100%', boxSizing: 'border-box', padding: 8 }}
      />
      <div style={{ marginTop: 12 }}>
        <button
          type="button"
          disabled={busy || !requirement}
          onClick={run}
          style={{ padding: '8px 20px', cursor: 'pointer' }}
        >
          {busy ? '生成中...' : '生成'}
        </button>
        <a
          href="/z-lc/report"
          onClick={(e) => { e.preventDefault(); navigate('/z-lc/report') }}
          style={{ marginLeft: 16 }}
        >
          ← 首页
        </a>
      </div>
      <pre
        style={{
          background: '#fafafa',
          border: '1px solid #f0f0f0',
          borderRadius: 8,
          padding: 12,
          marginTop: 16,
          minHeight: 80,
          whiteSpace: 'pre-wrap',
        }}
      >
        {log.length > 0 ? log.join('\n') : '执行日志'}
      </pre>
    </div>
  )
}
