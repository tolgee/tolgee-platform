import { Dispatch, SetStateAction, useCallback, useState } from 'react';

type Props = {
  initial: string | undefined;
  key: string;
  derive?: (value: string | undefined, isInitial: boolean) => void;
};

export function useLocalStorageState({ initial, key, derive }: Props) {
  const [value, _setValue] = useState<string | undefined>(() => {
    try {
      const storedValue = localStorage.getItem(key);
      if (storedValue) {
        return storedValue;
      } else {
        return initial;
      }
    } catch (e) {
      return initial;
    }
  });

  function setLocalStorageValue(value: string | undefined) {
    if (value === undefined) {
      localStorage.removeItem(key);
    } else {
      localStorage.setItem(key, value);
    }
  }

  const setValue: Dispatch<SetStateAction<string | undefined>> = useCallback(
    (valueOrFunction) => {
      if (typeof valueOrFunction === 'function') {
        return _setValue((previousValue) => {
          const newValue = (valueOrFunction as any)(previousValue);
          setLocalStorageValue(newValue);
          derive?.(newValue, false);
          return newValue;
        });
      } else {
        setLocalStorageValue(valueOrFunction);
        derive?.(valueOrFunction, false);
        return _setValue(valueOrFunction);
      }
    },
    [_setValue]
  );
  derive?.(value, true);
  return [value, setValue, setLocalStorageValue] as const;
}
