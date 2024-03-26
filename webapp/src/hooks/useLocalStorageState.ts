import { Dispatch, SetStateAction, useCallback, useState } from 'react';

type Props<T> = {
  initial: T;
  key: string;
  derive?: (value: T, isInitial: boolean) => void;
};

export function useLocalStorageState<T>({ initial, key, derive }: Props<T>) {
  const [value, _setValue] = useState<T>(() => {
    try {
      const storedValue = localStorage.getItem(key);
      if (storedValue) {
        return JSON.parse(storedValue);
      } else {
        return initial;
      }
    } catch (e) {
      return initial;
    }
  });

  function setLocalStorageValue(value: T) {
    if (value === undefined) {
      localStorage.removeItem(key);
    } else {
      localStorage.setItem(key, JSON.stringify(value));
    }
  }

  const setValue: Dispatch<SetStateAction<T>> = useCallback(
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
