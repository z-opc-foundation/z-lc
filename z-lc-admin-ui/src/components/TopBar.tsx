import { Link, useLocation } from 'react-router-dom';
import { Badge, Segmented, Space, Tag, Tooltip, Typography } from 'antd';
import { useQuery } from '@tanstack/react-query';
import { getHealth } from '@/api/health';
import { useUiStore, type Density } from '@/store/ui';

const { Text } = Typography;

/**
 * Console header: brand, live backend reachability pill, density switch.
 * Density changes the query cache freshness, not just CSS, because a "compact"
 * console implies "refresh less, read faster".
 */
export function TopBar({ appSlot }: { appSlot?: React.ReactNode }) {
  const location = useLocation();
  const density = useUiStore((state) => state.density);
  const setDensity = useUiStore((state) => state.setDensity);

  const health = useQuery({
    queryKey: ['health'],
    queryFn: getHealth,
    staleTime: 60_000,
    retry: 0,
    refetchOnWindowFocus: true,
  });

  const online = health.isSuccess;

  return (
    <header className="zlc-topbar">
      <Link to="/apps" className="zlc-topbar__brand">
        <span className="zlc-topbar__brand-mark">lc</span>
        <span>z-lc 控制台</span>
      </Link>

      <Tooltip title={online ? '后端 /api/lc/health 可达' : `后端不可达: ${health.error?.message ?? '未知错误'}`}>
        <Badge
          status={online ? 'success' : 'error'}
          text={<Text type="secondary" style={{ fontSize: 12 }}>{online ? '服务在线' : '服务离线'}</Text>}
        />
      </Tooltip>

      {appSlot}

      <div className="zlc-topbar__spacer" />

      <Space size={8}>
        {appSlot ? null : <Tag bordered={false}>低代码平台</Tag>}
        <Segmented<Density>
          size="small"
          value={density}
          onChange={(value) => setDensity(value)}
          options={[
            { label: '紧凑', value: 'compact' },
            { label: '舒适', value: 'comfortable' },
          ]}
        />
        <Text type="secondary" style={{ fontSize: 12 }}>
          {location.pathname}
        </Text>
      </Space>
    </header>
  );
}
