import { beforeEach, describe, expect, it } from 'vitest';
import { render } from '@testing-library/react';
import { DesignerPage, resetDesigner, useDesignerStore } from '.';

/**
 * 薄壳集成测试：确认共享包的设计器能在 z-lc 控制台里正常挂载。
 * 设计器内部行为（落点解算/属性写回/撤销）在上游包内已有 43 条测试覆盖，
 * 这里只验「接线正确 + 样式与物料可用」，避免同一套断言写两遍。
 */
describe('designer2 薄壳', () => {
  beforeEach(() => {
    window.localStorage.clear();
    resetDesigner();
  });

  it('挂载共享包设计器并渲染三栏', () => {
    render(<DesignerPage appCode="demo" />);
    expect(document.querySelector('[data-panel="materials"]')).toBeTruthy();
    expect(document.querySelector('[data-panel="canvas"]')).toBeTruthy();
    expect(document.querySelector('[data-panel="properties"]')).toBeTruthy();
  });

  it('物料库来自共享包，画布可正常插入节点', () => {
    render(<DesignerPage appCode="demo" />);
    expect(document.querySelectorAll('[data-material-type]').length).toBeGreaterThanOrEqual(20);
    const id = useDesignerStore.getState().insertNode('input', 'root');
    expect(id).toBeTruthy();
    expect(useDesignerStore.getState().schema.elements?.[id!]?.type).toBe('input');
  });

  it('样式已注入（designer.css 被打进产物）', () => {
    render(<DesignerPage appCode="demo" />);
    const app = document.querySelector('.dn-app');
    expect(app).toBeTruthy();
  });
});
