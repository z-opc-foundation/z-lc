import { create } from 'zustand';
import { persist } from 'zustand/middleware';

export type Density = 'compact' | 'comfortable';

interface UiState {
  density: Density;
  navCollapsed: boolean;
  /** Codes of entities recently opened, most-recent-first (max 8). */
  recentEntities: { appCode: string; entityCode: string; label: string }[];
  setDensity: (density: Density) => void;
  toggleNav: () => void;
  rememberEntity: (entry: { appCode: string; entityCode: string; label: string }) => void;
}

/**
 * Client-only UI preferences. All *data* state lives in the URL + react-query;
 * this store only holds view chrome settings, persisted to localStorage.
 */
export const useUiStore = create<UiState>()(
  persist(
    (set, get) => ({
      density: 'compact',
      navCollapsed: false,
      recentEntities: [],
      setDensity: (density) => set({ density }),
      toggleNav: () => set({ navCollapsed: !get().navCollapsed }),
      rememberEntity: (entry) => {
        const rest = get().recentEntities.filter(
          (item) => !(item.appCode === entry.appCode && item.entityCode === entry.entityCode),
        );
        set({ recentEntities: [entry, ...rest].slice(0, 8) });
      },
    }),
    { name: 'z-lc-admin-ui' },
  ),
);
