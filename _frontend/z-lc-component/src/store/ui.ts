import { create } from 'zustand';

export type Density = 'compact' | 'comfortable';

interface UiState {
  density: Density;
  setDensity: (density: Density) => void;
}

export const useUiStore = create<UiState>((set) => ({
  density: 'comfortable',
  setDensity: (density) => set({ density }),
}));
