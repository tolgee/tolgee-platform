import { useCallback, useState } from 'react';

const SHORTCUTS_STORAGE_KEY = '__tolgee_hide_shortcuts';

export const useHideShortcuts = () => {
  const [state, setState] = useState(
    Boolean(localStorage.getItem(SHORTCUTS_STORAGE_KEY))
  );

  const updateState = useCallback(
    (value: boolean) => {
      localStorage.setItem(SHORTCUTS_STORAGE_KEY, value ? 'true' : '');
      setState(value);
    },
    [setState]
  );

  return [state, updateState] as const;
};
