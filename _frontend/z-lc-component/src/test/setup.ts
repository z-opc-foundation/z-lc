import '@testing-library/jest-dom/vitest';

/**
 * Node 24+ 自带一个实验性 globalThis.localStorage，在没有 --localstorage-file 时
 * 它的值是 undefined，会把 jsdom 的实现遮掉 —— 于是 `window.localStorage.clear()` 炸。
 * 这里补一个够用的内存实现，让测试不依赖这个环境细节（应用代码本身对所有
 * localStorage 访问都是 try/catch 的，所以补与不补都不影响被测行为）。
 */
function installStorage(target: unknown): void {
  const store = new Map<string, string>();
  const storage = {
    getItem: (key: string) => (store.has(key) ? store.get(key)! : null),
    setItem: (key: string, value: string) => {
      store.set(key, String(value));
    },
    removeItem: (key: string) => {
      store.delete(key);
    },
    clear: () => store.clear(),
    key: (index: number) => Array.from(store.keys())[index] ?? null,
    get length() {
      return store.size;
    },
  };
  try {
    Object.defineProperty(target, 'localStorage', { configurable: true, writable: true, value: storage });
  } catch {
    /* 某些环境该属性不可重定义，忽略 */
  }
}

installStorage(globalThis);
installStorage(window);

/**
 * antd / rc-* 在 jsdom 里缺的浏览器 API 补丁。
 * 只补"jsdom 确实没有、但组件库直接依赖"的部分，不额外伪造行为。
 */
if (!window.matchMedia) {
  Object.defineProperty(window, 'matchMedia', {
    writable: true,
    value: (query: string) => ({
      matches: false,
      media: query,
      onchange: null,
      addListener: () => undefined,
      removeListener: () => undefined,
      addEventListener: () => undefined,
      removeEventListener: () => undefined,
      dispatchEvent: () => false,
    }),
  });
}

class ResizeObserverPolyfill {
  observe(): void {
    /* jsdom 没有布局, 空实现即可让 rc-resize-observer 不崩 */
  }

  unobserve(): void {
    /* noop */
  }

  disconnect(): void {
    /* noop */
  }
}

if (!(globalThis as Record<string, unknown>).ResizeObserver) {
  (globalThis as Record<string, unknown>).ResizeObserver = ResizeObserverPolyfill;
}

if (!globalThis.crypto) {
  (globalThis as Record<string, unknown>).crypto = { randomUUID: () => Math.random().toString(16).slice(2) };
}

// rc-virtual-list / Grid 会读这些, jsdom 一律给 0 而不是抛错
Object.defineProperty(window, 'scrollTo', { writable: true, value: () => undefined });

/**
 * jsdom 25 的 `Blob` 上没有 `text()`/`arrayBuffer()`（实测 `typeof f.text === 'undefined'`），
 * 而浏览器的 File 一直有。CSV 导入读文件用的就是 `file.text()`，所以这里补一个
 * 基于 FileReader 的等价实现 —— 补的是环境缺口，不改生产代码去迁就测试环境。
 */
if (typeof Blob !== 'undefined' && typeof Blob.prototype.text !== 'function') {
  Object.defineProperty(Blob.prototype, 'text', {
    configurable: true,
    writable: true,
    value(this: Blob): Promise<string> {
      return new Promise((resolve, reject) => {
        const reader = new FileReader();
        reader.onload = () => resolve(String(reader.result ?? ''));
        reader.onerror = () => reject(reader.error);
        reader.readAsText(this);
      });
    },
  });
}
