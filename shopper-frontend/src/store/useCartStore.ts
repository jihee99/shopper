import { create } from 'zustand';

interface CartState {
  itemCount: number;
  setItemCount: (count: number) => void;
  incrementCount: (amount?: number) => void;
  decrementCount: (amount?: number) => void;
  resetCount: () => void;
}

export const useCartStore = create<CartState>((set) => ({
  itemCount: 0,
  setItemCount: (count) => set({ itemCount: count }),
  incrementCount: (amount = 1) =>
    set((state) => ({ itemCount: state.itemCount + amount })),
  decrementCount: (amount = 1) =>
    set((state) => ({ itemCount: Math.max(0, state.itemCount - amount) })),
  resetCount: () => set({ itemCount: 0 }),
}));
