import { useEffect, useState } from "react";

export default function useLocalStorage(key, defaultValue) {
  const [state, setState] = useState(() => {
    const value = localStorage.getItem(key);
    return value ?? defaultValue;
  });

  useEffect(() => {
    localStorage.setItem(key, state);
  }, [state]);

  return [state, setState];
}
