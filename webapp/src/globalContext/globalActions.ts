import type { useGlobalActions } from './GlobalContext';

export const globalContext = {
  actions: undefined as ReturnType<typeof useGlobalActions> | undefined,
};
