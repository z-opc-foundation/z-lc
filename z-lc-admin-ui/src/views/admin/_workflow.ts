import { useEffect, useState } from 'react';
import type { WorkflowVocabulary } from '@/api/types';
import { getWorkflowVocabulary } from '@/api/workflowBinding';

/**
 * 触发时机的中文显示名。这份表只负责"把引擎给的事件码说得像人话"，
 * <b>不负责决定有哪些事件可选</b> —— 那份清单的唯一来源是 `/workflow-binding/vocabulary`
 * （缺陷 #61：页面此前自己抄了三个时机，其中两个引擎没有挂接点，保存成功而一个字都不执行）。
 * 认不出来的事件码原样显示，不猜、也不补一个"以后会支持"。
 */
const TRIGGER_LABELS: Record<string, string> = {
  AFTER_CREATE: '创建后',
};

export function triggerLabel(event: string | null | undefined): string {
  if (!event) return '—';
  const key = event.trim();
  return TRIGGER_LABELS[key] ?? key;
}

/** 词表这一读也有结局，和列表一样不许把"没读到"说成"没有"。 */
export interface VocabularySource {
  status: 'loading' | 'error' | 'ready';
  vocabulary: WorkflowVocabulary;
  error: unknown;
  reload: () => void;
}

const EMPTY_VOCABULARY: WorkflowVocabulary = { implemented: [], rejected: [] };

export function useWorkflowVocabulary(): VocabularySource {
  const [vocabulary, setVocabulary] = useState<WorkflowVocabulary>(EMPTY_VOCABULARY);
  const [status, setStatus] = useState<'loading' | 'error' | 'ready'>('loading');
  const [error, setError] = useState<unknown>(null);
  const [attempt, setAttempt] = useState(0);

  useEffect(() => {
    let stale = false;
    setStatus('loading');
    setError(null);
    void (async () => {
      let next: WorkflowVocabulary;
      try {
        next = await getWorkflowVocabulary();
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
