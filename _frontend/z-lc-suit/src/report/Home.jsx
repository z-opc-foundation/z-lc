import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Button, Card, Col, Row, Space, Typography } from 'antd'
import { RobotOutlined } from '@ant-design/icons'
import { EmptyState, ErrorState, LoadingState } from '@yuku123/z-frontend-common/ui'
import { listTemplates, applyTemplate } from './api.js'

const { Paragraph, Text } = Typography

/**
 * 报表模板画廊。apply 成功 → 跳转视图页。
 * 菜单 label「报表模板」已经是页面名，按 lead 008 §13 不再写页内标题。
 */
export default function Home() {
  const [templates, setTemplates] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [busy, setBusy] = useState('')
  const navigate = useNavigate()

  const load = () => {
    setLoading(true)
    setError(null)
    listTemplates()
      .then((list) => setTemplates(list || []))
      .catch(setError)
      .finally(() => setLoading(false))
  }

  useEffect(load, [])

  const onApply = async (tplId) => {
    setBusy(tplId)
    setError(null)
    try {
      const r = await applyTemplate(tplId)
      navigate(`/z-lc/report/view/${r.viewId}?bookId=${encodeURIComponent(r.bookId)}`)
    } catch (e) {
      setError(e)
    } finally {
      setBusy('')
    }
  }

  return (
    <div>
      <Space style={{ width: '100%', justifyContent: 'space-between', marginBottom: 16 }}>
        <Paragraph type="secondary" style={{ margin: 0 }}>
          从模板创建示例报表书，或交给 AI 一句话生成（需后端配置 z.report.llm.*）。
        </Paragraph>
        <Button type="primary" icon={<RobotOutlined />} onClick={() => navigate('/z-lc/report/ai')}>
          AI 一句话生成
        </Button>
      </Space>

      {error ? (
        <ErrorState
          error={error}
          title="报表模板没有读到"
          description="接口没有返回模板清单，下方空态不代表真的没有模板。"
          onRetry={load}
          style={{ marginBottom: 16 }}
        />
      ) : null}

      {loading ? (
        <LoadingState variant="cards" rows={3} />
      ) : templates.length === 0 ? (
        !error && (
          <EmptyState
            title="暂无报表模板"
            description="后端还没有注册内置模板；也可以先用 AI 一句话生成一张报表。"
            actionText="去 AI 生成"
            onAction={() => navigate('/z-lc/report/ai')}
          />
        )
      ) : (
        <Row gutter={[16, 16]}>
          {templates.map((t) => (
            <Col key={t.id} xs={24} sm={12} xl={8}>
              <Card
                hoverable
                title={t.title}
                styles={{ body: { minHeight: 88 } }}
                extra={
                  <Button
                    type="primary"
                    size="small"
                    loading={busy === t.id}
                    onClick={() => onApply(t.id)}
                  >
                    使用模板创建
                  </Button>
                }
              >
                <Text type="secondary">{t.description || '暂无描述'}</Text>
              </Card>
            </Col>
          ))}
        </Row>
      )}
    </div>
  )
}
