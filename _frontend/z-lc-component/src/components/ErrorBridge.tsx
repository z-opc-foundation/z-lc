import { useEffect } from 'react';
import { App } from 'antd';
import { subscribeToApiErrors } from '@/lc/utils/errorBus';

/**
 * Bridges transport-level failures into antd's contextual notification API.
 *
 * antd v5 requires the `App` wrapper for theme-aware `message`/`notification`,
 * so the fetch layer publishes to a bus instead of importing static methods.
 */
export function ErrorBridge() {
  const { notification } = App.useApp();

  useEffect(
    () =>
      subscribeToApiErrors((message, code) => {
        notification.error({
          message: code > 0 ? `请求失败 (${code})` : '请求失败',
          description: message,
          placement: 'bottomRight',
          duration: 4.5,
          style: { maxWidth: 420 },
        });
      }),
    [notification],
  );

  return null;
}
