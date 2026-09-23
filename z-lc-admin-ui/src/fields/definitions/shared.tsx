import type { CSSProperties, KeyboardEvent } from 'react';
import { Typography } from 'antd';

const { Text } = Typography;

/** Placeholder for a null/blank cell — consistent across every definition. */
export function NullCell() {
  return <Text type="secondary" style={{ opacity: 0.45 }}>—</Text>;
}

/** Single-line clamped cell text with the full value in a tooltip. */
export function ClampedText({ value, lines = 1 }: { value: string; lines?: number }) {
  return (
    <Text
      ellipsis={{ tooltip: value }}
      style={{ maxWidth: '100%', display: 'block', lineHeight: lines === 1 ? 1.5 : 1.4 }}
      title={value}
    >
      {value}
    </Text>
  );
}

/**
 * Keyboard contract for inline cell editing:
 * Enter commits, Escape cancels, Tab commits-and-advances.
 */
export function inlineKeyHandlers(commit: () => void, cancel: () => void) {
  return {
    onKeyDown: (event: KeyboardEvent<unknown>) => {
      switch (event.key) {
        case 'Enter':
          // Keep Enter inside multiline editors.
          if (event.shiftKey) return;
          event.preventDefault();
          commit();
          break;
        case 'Escape':
          event.preventDefault();
          event.stopPropagation();
          cancel();
          break;
        case 'Tab':
          commit();
          break;
        default:
          break;
      }
    },
  };
}

export const editorFillStyle: CSSProperties = { width: '100%' };
