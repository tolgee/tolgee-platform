import { createStore, useStore } from 'zustand';

interface TestClockState {
  time: number | undefined;
  setTime: (time: number | undefined) => void;
}

export const testClockStore = createStore<TestClockState>((set) => ({
  time: undefined as undefined | number,
  setTime: (time: number | undefined) => set((state) => ({ ...state, time })),
}));

function useTestClockStore<T>(selector?: (state: TestClockState) => T) {
  return useStore(testClockStore, selector!);
}

export const useTestClock = () => {
  return useTestClockStore((state) => state.time);
};

export const useResetTestClock = () => {
  const setTime = useTestClockStore((state) => state.setTime);
  return () => setTime(undefined);
};
