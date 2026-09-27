import { useEffect, useState } from 'react';
import type { DeploymentVocabulary } from '@/api/types';
import { getDeploymentVocabulary } from '@/api/deployment';

/**
 * 部署方式的中文显示名。这份表只负责"把服务器给的编码说得像人话"，
 * <b>不负责决定有哪些方式可选</b> —— 那份清单的唯一来源是 `/deployment/vocabulary`
 * （缺陷 #70：页面此前自己抄了三种方式，而三种一种都不执行）。
 * 认不出来的编码原样显示，不猜、也不补一个"以后会支持"。
 */
const TYPE_LABELS: Record<string, string> = {
  HOT_LOAD: '热加载',
  DOCKER: 'Docker 镜像',
  GIT_PUSH: 'Git 推送',
  SQL: 'SQL 脚本',
};

export function deployTypeLabel(type: string | null | undefined): string {
  if (!type) return '—';
  const key = type.trim();
  return TYPE_LABELS[key] ?? key;
}

/** 词表这一读也有结局，和列表一样不许把"没读到"说成"没有"。 */
export interface DeploymentVocabularySource {
  status: 'loading' | 'error' | 'ready';
  vocabulary: DeploymentVocabulary;
  error: unknown;
  reload: () => void;
}

const EMPTY_VOCABULARY: DeploymentVocabulary = { executable: [], rejected: [] };

export function useDeploymentVocabulary(): DeploymentVocabularySource {
  const [vocabulary, setVocabulary] = useState<DeploymentVocabulary>(EMPTY_VOCABULARY);
  const [status, setStatus] = useState<'loading' | 'error' | 'ready'>('loading');
  const [error, setError] = useState<unknown>(null);
  const [attempt, setAttempt] = useState(0);

  useEffect(() => {
    let stale = false;
    setStatus('loading');
    setError(null);
    void (async () => {
      let next: DeploymentVocabulary;
      try {
        next = await getDeploymentVocabulary();
      } catch (err) {
        if (stale) return;
        // 读失败要留在原地：空清单会让下拉框显示「暂无数据」，那是一句谎话。
        setVocabulary(EMPTY_VOCABULARY);
        setError(err);
        setStatus('error');
        return;
      }
      if (stale) return;
      setVocabulary(next);
      setStatus('ready');
    })();
    return () => {
      stale = true;
    };
  }, [attempt]);

  return { status, vocabulary, error, reload: () => setAttempt((prev) => prev + 1) };
}
