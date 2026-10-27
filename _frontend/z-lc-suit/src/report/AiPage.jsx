import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Alert, Button, Card, Input, Select, Space, Typography } from 'antd'
import { ThunderboltOutlined } from '@ant-design/icons'
import { aiDataset, aiView } from './api.js'

const { Text } = Typography

/**
 * AI 生成页 (M3): 一句话 → 数据集 → 视图。
 * 两步走: 先选数据源生成数据集, 再基于全部已注册数据集生成视图页。
 * 后端 LLM 未配置时, 返回配置指引错误 (前端原样透出, 见 ZReportBeanConfig 缺省实现)。
 * 菜单 label「AI 报表」已是页面名，按 lead 008 §13 不写页内标题。
 */
export default function AiPage() {
  const [sources, setSources] = useState([])
  const [sourcesError, setSourcesError] = useState(null)
  const [sourceId, setSourceId] = useState('')
  const [requirement, setRequirement] = useState('')
  const [log, setLog] = useState([])
  const [busy, setBusy] = useState(false)
  const navigate = useNavigate()

  const loadSources = () => {
    setSourcesError(null)
    // 数据源清单: 后端 /api/lc/report/datasource 端点 (DataSourceController);
    // 读不到只降级为手输 sourceId，不再静默吞掉错误（lead 008 §17）。
    fetch('/api/lc/report/datasource')
      .then((r) => (r.ok ? r.json() : Promise.reject(new Error(`HTTP ${r.status}`))))
      .then((list) => {
        const arr = Array.isArray(list) ? list : []
        setSources(arr)
        if (arr.length > 0) setSourceId((cur) => cur || arr[0].id)
      })
      .catch(setSourcesError)
  }

  useEffect(loadSources, [])

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
    <div style={{ maxWidth: 760 }}>
      {sourcesError ? (
        <Alert
          type="warning"
          showIcon
          style={{ marginBottom: 16 }}
          message="数据源清单没有读到"
          description={`${sourcesError.message || sourcesError}。可以直接手输数据源 ID 继续生成。`}
          action={
            <Button size="small" onClick={loadSources}>
              重试
            </Button>
          }
        />
      ) : null}

      <Card title="一句话生成报表">
        <Space direction="vertical" size="middle" style={{ width: '100%' }}>
          <div>
            <Text type="secondary" style={{ display: 'block', marginBottom: 6 }}>
              数据源
            </Text>
            {sources.length > 0 ? (
              <Select
                style={{ width: '100%' }}
                value={sourceId || undefined}
                onChange={setSourceId}
                placeholder="选择已注册的数据源"
                options={sources.map((s) => ({ value: s.id, label: `${s.id} (${s.type})` }))}
              />
            ) : (
              <Input
                value={sourceId}
                onChange={(e) => setSourceId(e.target.value)}
                placeholder="手输数据源 ID（清单读不到时可用）"
              />
            )}
          </div>
          <div>
            <Text type="secondary" style={{ display: 'block', marginBottom: 6 }}>
              需求描述
            </Text>
            <Input.TextArea
              value={requirement}
              onChange={(e) => setRequirement(e.target.value)}
              placeholder="例: 展示各城市销售额柱状图, 加一个销售总额 KPI"
              autoSize={{ minRows: 3, maxRows: 6 }}
            />
          </div>
          <Space>
            <Button
              type="primary"
              icon={<ThunderboltOutlined />}
              loading={busy}
              disabled={!requirement}
              onClick={run}
            >
              生成
            </Button>
            <Button type="link" onClick={() => navigate('/z-lc/report')}>
              返回模板列表
            </Button>
          </Space>
          <pre
            style={{
              background: '#fafafa',
              border: '1px solid #f0f0f0',
              borderRadius: 8,
              padding: 12,
              margin: 0,
              minHeight: 88,
              whiteSpace: 'pre-wrap',
            }}
          >
            {log.length > 0 ? log.join('\n') : '执行日志'}
          </pre>
        </Space>
      </Card>
    </div>
  )
}
