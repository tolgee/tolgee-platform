import { Dispatch, SetStateAction, useCallback, useState } from 'react';

type Props = {
  initial: string | undefined;
  key: string;
  derive?: (value: string | undefined, isInitial: boolean) => void;
};

export function useLocalStorageState<T extends string | undefined>({
  initial,
  key,
  derive,
}: Props) {
  function getLocalStorageValue() {
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
  }

  function setLocalStorageValue(value: string | undefined | null) {
    if (value === undefined || value === null) {
      localStorage.removeItem(key);
    } else {
      localStorage.setItem(key, value);
    }
  }

  const [value, _setValue] = useState<string | undefined>(() =>
    getLocalStorageValue()
  );

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
  return [
    value as T,
    setValue as Dispatch<SetStateAction<T>>,
    getLocalStorageValue,
  ] as const;
}
